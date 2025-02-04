package server.web.root.api;

import org.sqlite.SQLiteException;
import server.db.RoConn;
import server.db.RoTransaction;
import server.db.RwConn;
import server.db.RwTransaction;
import server.web.MailServer;
import server.web.Util;
import server.web.annotations.*;
import server.web.annotations.url.Path;
import server.web.param.misc.IpHandler;
import server.web.param.auth.RequireSession;
import server.web.param.auth.UserSession;
import server.web.param.misc.UserAgentHandler;
import server.web.route.ClientError;
import util.SqlSerde;

import javax.mail.Message;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class AccountAPI {

    public record UserInfo(
            int id,
            String name,
            String email,
            String bio,
            boolean admin,
            Boolean has_analytics
    ){}

    @Route
    public static @Json UserInfo all_userinfo(@FromRequest(RequireSession.class) UserSession auth, RoConn conn) throws SQLException {
        try(var stmt = conn.namedPreparedStatement("select name, bio from users where id=:id")){
            stmt.setInt(":id", auth.user_id);
            var rs = stmt.executeQuery();
            return new UserInfo(
                    auth.user_id,
                    rs.getString("name"),
                    auth.email,
                    rs.getString("bio"),
                    auth.admin,
                    auth.has_analytics
            );
        }
    }

    public static class DeleteAccount{
        public String email;
        public String password;
    }

    @Route
    public static void delete_account(@FromRequest(RequireSession.class) UserSession auth, RoTransaction trans, @Body @Json DeleteAccount account) throws SQLException, ClientError.Unauthorized {
        if(!account.email.equals(auth.email))
            throw new ClientError.Unauthorized("Incorrect email");
        account.password = Util.hashy((account.password+"\0\0\0\0"+account.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("delete from users where email=:email AND pass=:pass")){
            stmt.setString(":email", auth.email);
            stmt.setString(":pass", account.password);
            if(stmt.executeUpdate() != 1)
                throw new ClientError.Unauthorized("Incorrect password");
        }
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
            stmt.setInt(":id", auth.user_id);
            stmt.setString(":old_password", cp.old_password);
            stmt.setString(":new_password", cp.new_password);
            stmt.execute();
        }

        try(var stmt = trans.namedPreparedStatement("delete from sessions where user_id=:id")){
            stmt.setInt(":id", auth.user_id);
            stmt.execute();
        }
    }

    public static class Register{
        public String name;
        public String email;
        public String password;
    }

    @Route
    public static void register(MailServer mail, RwTransaction trans, @Body @Json Register register) throws SQLException, ClientError.BadRequest{
        register.password = Util.hashy((register.password+"\0\0\0\0"+register.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("insert into users values(null, :name, :email, :pass, false, null, null, null)")){
            stmt.setString(":name", register.name);
            stmt.setString(":email", register.email);
            stmt.setString(":pass", register.password);
            stmt.execute();
        }catch (SQLiteException e){
            if(e.getResultCode().code==2067){//UNIQUE CONSTRAINTS FAILED
                throw new ClientError.BadRequest("Account with that email already exists");
            }
            throw e;
        }

        mail.sendMail(message -> {
            message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(register.email));
            message.setSubject("Welcome!");
            message.setContent("Thank you for registering for an account " + register.name, "text/html");
        });
    }

    public static class Login{
        public String email;
        public String password;
    }

    @Route
    public static String login(MailServer mail, @FromRequest(IpHandler.class)InetAddress ip, @FromRequest(UserAgentHandler.class)String agent, RwTransaction trans,  @Body @Json Login login) throws SQLException, ClientError.Unauthorized {
        int user_id;
        login.password = Util.hashy((login.password+"\0\0\0\0"+login.email).getBytes());
        try(var stmt = trans.namedPreparedStatement("select id from users where email=:email AND pass=:pass")){
            stmt.setString(":email", login.email);
            stmt.setString(":pass", login.password);
            var res = stmt.executeQuery();
            if(!res.next())
                throw new ClientError.Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
            try{
                user_id = res.getInt(1);
            }catch (SQLException ignore){
                throw new ClientError.Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
            }
        }

        int session_id;
        try(var stmt = trans.namedPreparedStatement("insert into sessions values(null, null, :user_id, :exp, :agent, :ip) returning id")){
            stmt.setInt(":user_id", user_id);
            stmt.setLong(":exp", new Date().getTime() + 2628000000L);
            stmt.setString(":agent", agent);
            stmt.setString(":ip", ip.getHostAddress());
            session_id = stmt.executeQuery().getInt(1);
        }

        var hash = Util.hashy((login.email + "\0\0\0\0" + login.password + "\0\0\0\0" + session_id).getBytes());
        var token = String.format("%s%08X", hash, session_id);

        try(var stmt = trans.namedPreparedStatement("update sessions set token=:token where id=:id")){
            stmt.setString(":token", token);
            stmt.setInt(":id", session_id);
            stmt.execute();
        }

        mail.sendMail(message -> {
            Util.LocationQuery res = null;
            try{
                res = Util.queryLocation(ip);
            }catch (Exception ignore){}
            message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(login.email));
            message.setSubject("Warning");

            message.setContent("Someone logged into your account <br/>IP: " + ip + "<br/>User Agent: " + agent, "text/html");
        });

        return token;
    }

    public static class Session{
        public int id;
        public long expiration;
        public String agent;
        public String ip;
    }

    @Route
    public static @Json List<Session> list_sessions(@FromRequest(RequireSession.class) UserSession auth, RoConn conn) throws SQLException {
        try(var stmt = conn.namedPreparedStatement("select * from sessions where user_id=:id")){
            stmt.setInt(":id", auth.user_id);
            return SqlSerde.sqlList(stmt.executeQuery(), Session.class);
        }
    }

    @Route("/invalidate_session/<session_id>")
    public static void invalidate_session(@FromRequest(RequireSession.class) UserSession auth, RwConn conn, @Path int session_id) throws SQLException, ClientError.BadRequest {
        try(var stmt = conn.namedPreparedStatement("delete from sessions where id=:session_id AND user_id=:user_id")){
            stmt.setInt(":session_id", session_id);
            stmt.setInt(":user_id", auth.user_id);
            if(stmt.executeUpdate() != 1)
                throw new ClientError.BadRequest("Could not invalidate session, session does not belong to you or does not exist");
        }
    }

}
