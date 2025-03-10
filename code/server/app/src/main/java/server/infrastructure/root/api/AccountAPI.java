package server.infrastructure.root.api;

import framework.db.DbManager;
import framework.web.TimedEvents;
import framework.web.WebServer;
import framework.web.annotations.*;
import framework.web.annotations.url.Nullable;
import org.sqlite.SQLiteException;
import framework.db.RoConn;
import framework.db.RoTransaction;
import framework.db.RwTransaction;
import framework.web.error.BadRequest;
import framework.web.error.Unauthorized;
import server.infrastructure.DbManagerImpl;
import server.infrastructure.param.auth.OptionalAuth;
import server.infrastructure.param.auth.SessionCache;
import server.mail.MailServer;
import framework.web.Util;
import framework.web.annotations.url.Path;
import framework.web.param.misc.IpHandler;
import server.infrastructure.param.auth.RequireSession;
import server.infrastructure.param.auth.UserSession;
import framework.web.param.misc.UserAgentHandler;
import framework.util.SqlSerde;

import javax.mail.Message;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Routes
public class AccountAPI {

    public sealed interface UserInfo permits PrivateUserInfo, PublicUserInfo {}

    public record PublicUserInfo(
        long id,
        String name,
        boolean organizer,
        String bio,
        String disp_email,
        String disp_phone_number,
        long picture,
        long banner
    ) implements UserInfo {}

    public record PrivateUserInfo(
        long id,
        String name,
        String email,
        boolean organizer,
        boolean admin,
        String bio,
        String disp_email,
        String disp_phone_number,
        long picture,
        long banner
    ) implements UserInfo {}

    @Route("/userinfo/<id>")
    public static @Json UserInfo userinfo(@FromRequest(OptionalAuth.class) UserSession auth, @Path @Nullable Long id, RoTransaction trans) throws SQLException, Unauthorized {
        UserInfo result;
        if(auth==null || (id!=null && auth.user_id!=id)){
            if(id==null)
                throw new Unauthorized("No identification present");
            try(trans; var stmt = trans.namedPreparedStatement("select name, email, bio, organizer, disp_email, disp_phone_number, picture, banner from users where id=:id")){
                stmt.setLong(":id", id);
                var rs = stmt.executeQuery();
                var organizer = rs.getBoolean("organizer");
                var disp_email = rs.getString("disp_email");
                if(disp_email==null && organizer)
                    disp_email = rs.getString("email");
                result = new PublicUserInfo(
                        id,
                        rs.getString("name"),
                        organizer,
                        rs.getString("bio"),
                        rs.getString("disp_phone_number"),
                        disp_email,
                        rs.getLong("picture"),
                        rs.getLong("banner")
                );
            }
        }else{
            try(trans; var stmt = trans.namedPreparedStatement("select name, bio, disp_email, disp_phone_number, picture, banner from users where id=:id")){
                stmt.setLong(":id", auth.user_id);
                var rs = stmt.executeQuery();
                result = new PrivateUserInfo(
                        auth.user_id,
                        rs.getString("name"),
                        auth.email,
                        auth.organizer,
                        auth.admin,
                        rs.getString("bio"),
                        rs.getString("disp_phone_number"),
                        rs.getString("disp_email"),
                        rs.getLong("picture"),
                        rs.getLong("banner")
                );
            }
        }

        return result;
    }

    public static class Register{
        public String name;
        public String email;
        public String password;
    }

    @Route
    public static void register(MailServer mail, RwTransaction trans, @Body @Json Register register) throws SQLException, BadRequest {
        register.password = Util.hashy((register.password+"\0\0\0\0"+register.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("insert into users values(null, :name, :email, :pass, false, false, null, null, null, null, null)")){
            stmt.setString(":name", register.name);
            stmt.setString(":email", register.email);
            stmt.setString(":pass", register.password);
            if(stmt.executeUpdate()!=1)
                throw new RuntimeException("Account couldn't be created");
        }catch (SQLiteException e){
            if(e.getResultCode().code==2067){//UNIQUE CONSTRAINTS FAILED
                throw new BadRequest("Account with that email already exists");
            }
            throw e;
        }
        trans.commit();

        mail.sendMail(message -> {
            message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(register.email));
            message.setSubject("Welcome!");
            message.setContent("Thank you for registering for an account " + register.name, "text/html");
        });
    }

    @Routes
    public static class SessionModifiers{

        @OnMount
        public static void init(WebServer server){
            var db = server.getManagedState(DbManagerImpl.class);

            var cache = new SessionCache();
            server.addManagedState(cache);
            db.addUpdateHook((type, database, table, rowId) -> {
                if (table.equals("sessions")) {
                    switch (type) {
                        case DELETE, UPDATE -> cache.invalidate_session(rowId);
                        case INSERT -> {
                        }
                    }
                }
            });

            var timer = server.getManagedState(TimedEvents.class);
            timer.addMinutely(() -> {
                try(var trans = db.rw_transaction(">session_timer")){
                    try(var stmt = trans.namedPreparedStatement("delete from sessions where expiration<:now")){
                        stmt.setLong(":now", new Date().getTime());
                        stmt.execute();
                    }
                    trans.commit();
                    Logger.getGlobal().log(Level.FINE, "Ran session expiration clear");
                }catch (Exception e){
                    Logger.getGlobal().log(Level.SEVERE, "Failed to run session expiration clear", e);
                }
            });
        }



        public static class Login{
            public String email;
            public String password;
        }

        @Route
        public static String login(MailServer mail, @FromRequest(IpHandler.class)InetAddress ip, @FromRequest(UserAgentHandler.class)String agent, RwTransaction trans,  @Body @Json Login login) throws SQLException, Unauthorized {
            long user_id;
            login.password = Util.hashy((login.password+"\0\0\0\0"+login.email).getBytes());
            try(var stmt = trans.namedPreparedStatement("select id from users where email=:email AND pass=:pass")){
                stmt.setString(":email", login.email);
                stmt.setString(":pass", login.password);
                try(var res = stmt.executeQuery()){
                    if(!res.next())
                        throw new Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
                    try{
                        user_id = res.getLong(1);
                    }catch (SQLException ignore){
                        throw new Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
                    }
                }
            }

            long session_id;
            try(var stmt = trans.namedPreparedStatement("insert into sessions values(null, null, :user_id, :exp, :agent, :ip) returning id")){
                stmt.setLong(":user_id", user_id);
                stmt.setLong(":exp", new Date().getTime() + 2628000000L);
                stmt.setString(":agent", agent);
                stmt.setString(":ip", ip.getHostAddress());
                try(var res = stmt.executeQuery()){
                    session_id = res.getLong(1);
                }
            }

            var hash = Util.hashy((login.email + "\0\0\0\0" + login.password + "\0\0\0\0" + session_id + "\0\0\0\0" + System.nanoTime()).getBytes());
            var token = String.format("%s%08X", hash, session_id);

            try(var stmt = trans.namedPreparedStatement("update sessions set token=:token where id=:id")){
                stmt.setString(":token", token);
                stmt.setLong(":id", session_id);
                stmt.execute();
            }

            trans.commit();

            mail.sendMail(message -> {
                Util.LocationQuery res = null;
                try{
                    res = Util.queryLocation(ip);
                }catch (Exception ignore){}
                message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(login.email));
                message.setSubject("Warning");

                message.setContent("Someone logged into your account <br/>IP: " + ip + "<br/>User Agent: " + agent, "text/html");
            });

            Logger.getGlobal().log(Level.FINER, "User: " + login.email + " Logged in with session: " + token);

            return token;
        }

        public static class Session{
            public long id;
            public long expiration;
            public String agent;
            public String ip;
        }

        @Route
        public static @Json List<Session> list_sessions(@FromRequest(RequireSession.class) UserSession auth, RoConn conn) throws SQLException {
            List<Session> result;
            try(var stmt = conn.namedPreparedStatement("select * from sessions where user_id=:id")){
                stmt.setLong(":id", auth.user_id);
                result = SqlSerde.sqlList(stmt.executeQuery(), Session.class);
            }
            conn.close();
            return result;
        }

        @Route("/invalidate_session/<session_id>")
        public static void invalidate_session(@FromRequest(RequireSession.class) UserSession auth, RwTransaction trans, @Path long session_id) throws SQLException, BadRequest {
            try(var stmt = trans.namedPreparedStatement("delete from sessions where id=:session_id AND user_id=:user_id")){
                stmt.setLong(":session_id", session_id);
                stmt.setLong(":user_id", auth.user_id);
                if(stmt.executeUpdate() != 1)
                    throw new BadRequest("Could not invalidate session, session does not belong to you or does not exist");
            }
            trans.commit();
        }


        public static class DeleteAccount{
            public String email;
            public String password;
        }

        @Route
        public static void delete_account(@FromRequest(RequireSession.class) UserSession auth, RwTransaction trans, @Body @Json DeleteAccount account) throws SQLException, Unauthorized {
            if(!account.email.equals(auth.email))
                throw new Unauthorized("Incorrect email");
            account.password = Util.hashy((account.password+"\0\0\0\0"+account.email).getBytes());
            try(var stmt = trans.namedPreparedStatement("delete from users where email=:email AND pass=:pass")){
                stmt.setString(":email", auth.email);
                stmt.setString(":pass", account.password);
                if(stmt.executeUpdate() != 1)
                    throw new Unauthorized("Incorrect password");
            }
            trans.commit();
        }

        public static class ChangePassword{
            public String email;
            public String old_password;
            public String new_password;
        }

        @Route
        public static void change_password(@FromRequest(RequireSession.class) UserSession auth, RoTransaction trans, @Body @Json ChangePassword cp) throws SQLException {
            cp.old_password = Util.hashy((cp.old_password+"\0\0\0\0"+cp.email).getBytes());
            cp.new_password = Util.hashy((cp.new_password+"\0\0\0\0"+cp.email).getBytes());
            try(var stmt = trans.namedPreparedStatement("update users set password=:new_password where email=:email AND password=:old_password AND id=:id")){
                stmt.setString(":email", cp.email);
                stmt.setLong(":id", auth.user_id);
                stmt.setString(":old_password", cp.old_password);
                stmt.setString(":new_password", cp.new_password);
                stmt.execute();
            }

            try(var stmt = trans.namedPreparedStatement("delete from sessions where user_id=:id")){
                stmt.setLong(":id", auth.user_id);
                stmt.execute();
            }
            trans.commit();
        }

    }

}
