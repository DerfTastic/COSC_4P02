package server.infrastructure.root.api;

import com.alibaba.fastjson2.JSONReader;
import framework.util.Tuple;
import framework.web.TimedEvents;
import framework.web.WebServer;
import framework.web.annotations.*;
import framework.web.annotations.http.Delete;
import framework.web.annotations.url.Nullable;
import framework.web.request.Request;
import framework.web.route.RouteParameter;
import org.sqlite.SQLiteException;
import framework.db.RoConn;
import framework.db.RoTransaction;
import framework.db.RwTransaction;
import framework.web.error.BadRequest;
import framework.web.error.Unauthorized;
import server.Config;
import server.infrastructure.DbManagerImpl;
import server.infrastructure.DynamicMediaHandler;
import server.infrastructure.param.auth.*;
import server.mail.MailServer;
import framework.web.Util;
import framework.web.annotations.url.Path;
import framework.web.param.misc.IpHandler;
import framework.web.param.misc.UserAgentHandler;
import framework.util.SqlSerde;
import server.mail.MessageConfigurator;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Routes
public class AccountAPI {

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


        if(Config.CONFIG.send_mail_on_register)
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
    @Delete
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
    public static void delete_account(@FromRequest(RequireSession.class) UserSession auth, RwTransaction trans, @Body @Json DeleteAccount account, DynamicMediaHandler media) throws SQLException, Unauthorized {
        record DeleteResult(long picture, long banner){}

        if(!account.email.equals(auth.email))
            throw new Unauthorized("Incorrect email");
        account.password = Util.hashy((account.password+"\0\0\0\0"+account.email).getBytes());

        DeleteResult res;
        try(var stmt = trans.namedPreparedStatement("delete from users where email=:email AND pass=:pass returning picture, banner")){
            stmt.setString(":email", auth.email);
            stmt.setString(":pass", account.password);
            res = SqlSerde.sqlSingle(stmt.executeQuery(), rs -> new DeleteResult(rs.getLong("picture"), rs.getLong("picture")));
        }
        trans.commit();
        if(res.picture!=0)
            media.delete(res.picture);
        if(res.banner!=0)
            media.delete(res.banner);
    }

    public static class ChangeAuth {
        public String old_email;
        public String new_email;
        public String old_password;
        public String new_password;
    }

    @Route
    public static void change_auth(@FromRequest(RequireSession.class) UserSession auth, RoTransaction trans, @Body @Json ChangeAuth ca) throws SQLException, BadRequest {
        if(ca.new_password==null&&ca.new_email==null)
            throw new BadRequest("Nothing to change");

        if(ca.new_password==null)ca.new_password = ca.old_password;
        if(ca.new_email==null)ca.new_email = ca.old_email;

        ca.old_password = Util.hashy((ca.old_password+"\0\0\0\0"+ca.old_email).getBytes());
        ca.new_password = Util.hashy((ca.new_password+"\0\0\0\0"+ca.new_email).getBytes());
        try(var stmt = trans.namedPreparedStatement("update users set password=:new_password, email=:new_email where email=:old_email AND password=:old_password AND id=:id")){
            stmt.setString(":old_email", ca.old_email);
            stmt.setString(":new_email", ca.new_email);
            stmt.setLong(":id", auth.user_id);
            stmt.setString(":old_password", ca.old_password);
            stmt.setString(":new_password", ca.new_password);
            stmt.execute();
        }

        try(var stmt = trans.namedPreparedStatement("delete from sessions where user_id=:id")){
            stmt.setLong(":id", auth.user_id);
            stmt.execute();
        }
        trans.commit();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class UpdateUser{
        String name;
        Optional<String> bio;
        Optional<String> disp_phone_number;
        Optional<String> disp_email;

        private static String requireString(JSONReader reader){
            if(reader.isString())
                return reader.readString();
            else
                throw new RuntimeException("Expected String value");
        }

        private static Optional<String> optionalString(JSONReader reader){
            if(reader.nextIfNull())
                return Optional.empty();
            if(reader.isString())
                return Optional.of(reader.readString());
            else
                throw new RuntimeException("Expected String value");
        }

        public UpdateUser(JSONReader reader) throws BadRequest {
            if(!reader.nextIfObjectStart())
                throw new BadRequest("Expected an object");
            while(!reader.nextIfObjectEnd()){
                if(!reader.isString())
                    throw new BadRequest("Expected field");
                var field_name = reader.readString();
                if(!reader.nextIfMatch(':'))
                    throw new BadRequest("Expected colon");
                switch(field_name){
                    case "name" -> this.name = requireString(reader);
                    case "bio" -> this.bio = optionalString(reader);
                    case "disp_phone_number" -> this.disp_phone_number = optionalString(reader);
                    case "disp_email" -> this.disp_email = optionalString(reader);
                    default -> throw new BadRequest("Unknown field: " + field_name);
                }
            }
        }
    }

    public static class UpdateUserFromRequest implements RouteParameter<UpdateUser> {
        @Override
        public UpdateUser construct(Request request) throws Exception {
            try(var reader = JSONReader.of(request.exchange.getRequestBody().readAllBytes())){
                return new UpdateUser(reader);
            }
        }
    }

    @SuppressWarnings("OptionalAssignedToNull")
    @Route
    public static void update_user(@FromRequest(RequireSession.class) UserSession auth, RwTransaction trans, @FromRequest(UpdateUserFromRequest.class) UpdateUser update) throws SQLException, BadRequest {
        var str = new StringBuilder().append("update users set ");

        if(update.name!=null)
            str.append("name=:name,");
        if(update.bio!=null)
            str.append("bio=:bio,");
        if(update.disp_email!=null)
            str.append("disp_email=:disp_email,");
        if(update.disp_phone_number!=null)
            str.append("disp_phone_number=:disp_phone_number,");

        if(str.charAt(str.length()-1)!=',') return;
        str.deleteCharAt(str.length()-1);
        str.append(" where id=:id");

        try(var stmt = trans.namedPreparedStatement(str.toString())){
            stmt.setLong(":id", auth.user_id);

            if(update.name!=null)
                stmt.setString(":name", update.name);
            if(update.bio!=null&&update.bio.isPresent())
                stmt.setString(":bio", update.bio.get());
            if(update.disp_email!=null&&update.disp_email.isPresent())
                stmt.setString(":disp_email", update.disp_email.get());
            if(update.disp_phone_number!=null&&update.disp_phone_number.isPresent())
                stmt.setString(":disp_phone_number", update.disp_phone_number.get());

            if(stmt.executeUpdate()!=1)
                throw new BadRequest("Failed to update user");
        }
        trans.commit();
    }

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

        if(Config.CONFIG.send_mail_on_register)
            mail.sendMail(message -> {
                message.setRecipients(Message.RecipientType.TO, MailServer.fromStrings(register.email));
                message.setSubject("Welcome!");
                message.setContent("Thank you for registering for an account " + register.name, "text/html");
            });
    }


    @Route
    public static long set_user_picture(@FromRequest(RequireSession.class)UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Body byte[] data) throws SQLException, BadRequest {
        // 10 MiB
        if(data.length > (1<<20)*10){
            throw new BadRequest("File too large, maximum file size is 10 MiB");
        }

        var media_id = handler.add(data);
        long old_media = 0;
        try{
            try(var stmt = trans.namedPreparedStatement("select picture from users where id=:id")){
                stmt.setLong(":id", session.user_id);
                old_media = stmt.executeQuery().getLong(1);
            }

            try(var stmt = trans.namedPreparedStatement("update users set picture=:picture where id=:id")){
                stmt.setLong(":id", session.user_id);
                stmt.setLong(":picture", media_id);
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Failed to set picture for user");
            }
            trans.commit();
        }catch (Exception e){
            trans.commit();
            if(media_id!=0){
                handler.delete(media_id);
            }
        }
        if(old_media!=0){
            handler.delete(old_media);
        }

        return media_id;
    }

    @Route
    public static long set_user_banner_picture(@FromRequest(RequireSession.class)UserSession session, RwTransaction trans, DynamicMediaHandler handler, @Body byte[] data) throws SQLException, BadRequest {
        // 10 MiB
        if(data.length > (1<<20)*10){
            throw new BadRequest("File too large, maximum file size is 10 MiB");
        }

        var media_id = handler.add(data);
        long old_media = 0;
        try{
            try(var stmt = trans.namedPreparedStatement("select banner from users where id=:id")){
                stmt.setLong(":id", session.user_id);
                old_media = stmt.executeQuery().getLong("banner");
            }

            try(var stmt = trans.namedPreparedStatement("update users set banner=:banner where id=:id")){
                stmt.setLong(":id", session.user_id);
                stmt.setLong(":banner", media_id);
                if(stmt.executeUpdate()!=1)
                    throw new BadRequest("Failed to set banner picture for user");
            }
            trans.commit();
        }catch (Exception e){
            trans.commit();
            if(media_id!=0){
                handler.delete(media_id);
            }
        }
        if(old_media!=0){
            handler.delete(old_media);
        }

        return media_id;
    }


    public record UserId(String email, long id){}
    public static class PasswordResetManager{
        private HashMap<String, UserId> h1;
        private HashMap<String, UserId> h2;

        public synchronized UserId get(String token){
            var res = h1.get(token);
            if(res!=null)return res;
            return h2.get(token);
        }

        public synchronized void tick(){
            h2.clear();
            var tmp = h2;
            h2 = h1;
            h1 = tmp;
        }
    }

    @OnMount
    public static void init_reset_password_token_store(WebServer server){
        var timer = server.getManagedState(TimedEvents.class);
        var prm = new PasswordResetManager();
        server.addManagedState(prm);
        timer.addMinutely(prm::tick);
    }

    @Route("/reset_password/<token>")
    public static void reset_password(MailServer mail, @Path @Nullable String token, PasswordResetManager prm){


        if(token!=null){
            var rng = new byte[16];
            new SecureRandom().nextBytes(rng);
            String rngStr = Util.base64Str(rng);

            mail.sendMail(message -> {
//                message.set
            });
        }else{
            var rng = new byte[12];
            new SecureRandom().nextBytes(rng);
            String rngStr = Util.base64Str(rng);
        }
    }
}
