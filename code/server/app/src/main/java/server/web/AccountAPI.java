package server.web;

import org.sqlite.SQLiteException;
import server.db.DbConnection;
import server.db.DbManager;
import server.db.Transaction;
import server.web.annotations.Body;
import server.web.annotations.FromRequest;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.annotations.url.Path;
import server.web.route.ClientError;
import server.web.route.RouteImpl;
import server.web.route.RouteParameter;
import util.SqlSerde;

import javax.mail.Message;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class AccountAPI {

    public static class DeleteAccount{
        public String email;
        public String password;
    }

    @Route
    public static void delete_account(@FromRequest(UserAuthFromRequest.class) UserAuth auth, Transaction trans, @Body @Json DeleteAccount account) throws SQLException, ClientError.Unauthorized {
        if(!account.email.equals(auth.email))
            throw new ClientError.Unauthorized("Incorrect email");
        account.password = Util.hashy((account.password+"\0\0\0\0"+account.email).getBytes());
        try(var stmt = trans.conn.namedPreparedStatement("delete from users where email=:email AND pass=:pass")){
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
    public static void change_password(@FromRequest(UserAuthFromRequest.class) UserAuth auth, Transaction trans, @Body @Json ChangePassword cp) throws SQLException {
        cp.old_password = Util.hashy((cp.old_password+"\0\0\0\0"+cp.email).getBytes());
        cp.new_password = Util.hashy((cp.new_password+"\0\0\0\0"+cp.email).getBytes());
        try(var stmt = trans.conn.namedPreparedStatement("update users set password=:new_password where email=:email AND password=:old_password AND id=:id")){
            stmt.setString(":email", cp.email);
            stmt.setInt(":id", auth.user_id);
            stmt.setString(":old_password", cp.old_password);
            stmt.setString(":new_password", cp.new_password);
            stmt.execute();
        }

        try(var stmt = trans.conn.namedPreparedStatement("delete from sessions where user_id=:id")){
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
    public static void register(MailServer mail, Transaction trans, @Body @Json Register register) throws SQLException, ClientError.BadRequest{
        register.password = Util.hashy((register.password+"\0\0\0\0"+register.email).getBytes());
        try(var stmt = trans.conn.namedPreparedStatement("insert into users values(null, :name, :email, :pass, null, null, null)")){
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
        String email;
        String password;
    }

    @Route
    public static String login(MailServer mail, RouteImpl.Request request, Transaction trans, @Body @Json Login login) throws SQLException, ClientError.Unauthorized, NoSuchAlgorithmException {
        int user_id;
        login.password = Util.hashy((login.password+"\0\0\0\0"+login.email).getBytes());
        try(var stmt = trans.conn.namedPreparedStatement("select id from users where email=:email AND pass=:pass")){
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

        var agent = request.exchange.getRequestHeaders().getFirst("User-Agent");
        var ip = request.exchange.getRemoteAddress().getAddress().getHostAddress();

        int session_id;
        try(var stmt = trans.conn.namedPreparedStatement("insert into sessions values(null, null, :user_id, :exp, :agent, :ip) returning id")){
            stmt.setInt(":user_id", user_id);
            stmt.setLong(":exp", new Date().getTime() + 2628000000L);
            stmt.setString(":agent", agent);
            stmt.setString(":ip", ip);
            session_id = stmt.executeQuery().getInt(1);
        }

        var hash = Util.hashy((login.email + "\0\0\0\0" + login.password + "\0\0\0\0" + session_id).getBytes());
        var token = String.format("%s%08X", hash, session_id);

        try(var stmt = trans.conn.namedPreparedStatement("update sessions set token=:token where id=:id")){
            stmt.setString(":token", token);
            stmt.setInt(":id", session_id);
            stmt.execute();
        }

        Util.LocationQuery res = null;
        try{
            res = Util.queryLocation(request.exchange.getRemoteAddress().getAddress());
        }catch (Exception ignore){}
        mail.sendMail(message -> {
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
    public static @Json List<Session> list_sessions(@FromRequest(UserAuthFromRequest.class) UserAuth auth, DbConnection conn) throws SQLException {
        try(var stmt = conn.namedPreparedStatement("select * from sessions where user_id=:id")){
            stmt.setInt(":id", auth.user_id);
            return SqlSerde.sqlList(stmt.executeQuery(), Session.class);
        }
    }

    @Route("/invalidate_session/<session_id>")
    public static void invalidate_session(@FromRequest(UserAuthFromRequest.class) UserAuth auth, DbConnection conn, @Path int session_id) throws SQLException {
        try(var stmt = conn.namedPreparedStatement("delete from sessions where id=:session_id AND user_id=:user_id")){
            stmt.setInt(":session_id", session_id);
            stmt.setInt(":user_id", auth.user_id);
            stmt.execute();
        }
    }

    public static class UserAuth{
        public int user_id;
        public int session_id;
        public String email;

        public int organizer_id;
        public int max_events;
        public boolean has_analytics;
    }

    public static class UserAuthFromRequest implements RouteParameter<UserAuth>{

        @Override
        public UserAuth construct(RouteImpl.Request request) throws Exception {
            var token = request.exchange.getRequestHeaders().getFirst("X-UserAPIToken");
            if(token==null)throw new ClientError.Unauthorized("No valid session");
            try(var conn = request.getServer().getManagedResource(DbManager.class).conn()){
                try(var stmt = conn.namedPreparedStatement("delete from sessions where expiration<:now")){
                    stmt.setLong(":now", new Date().getTime());
                    stmt.execute();
                }
                try(var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id left join organizers on users.organizer_id=organizers.id where sessions.token=:token")){
                    stmt.setString(":token", token);
                    var result = stmt.executeQuery();
                    if(result==null||!result.next())throw new ClientError.Unauthorized("No valid session");

                    var auth = new UserAuth();
                    auth.session_id = result.getInt("id");
                    auth.user_id = result.getInt("user_id");
                    auth.email = result.getString("email");

                    auth.organizer_id = result.getInt("organizer_id");
                    auth.max_events = result.getInt("max_events");
                    auth.has_analytics = result.getBoolean("has_analytics");
                    return auth;
                }
            }
        }
    }
}
