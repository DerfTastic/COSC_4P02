package server.web;

import server.db.DbConnection;
import server.db.DbManager;
import server.db.Transaction;
import server.web.annotations.Body;
import server.web.annotations.FromRequest;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.route.ClientError;
import server.web.route.RouteImpl;
import server.web.route.RouteParameter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;

@SuppressWarnings("unused")
public class AccountAPI {

    public static class Register{
        String name;
        String email;
        String password;
    }

    @Route
    public static void register(Transaction trans, @Body @Json Register register) throws SQLException, ClientError.BadRequest{
        try(var stmt = trans.conn.namedPreparedStatement("insert into users values(null, :name, :email, :pass, null, null, null)")){
            stmt.setString(":name", register.name);
            stmt.setString(":email", register.email);
            stmt.setString(":pass", register.password);
            stmt.execute();
        }
    }

    public static class Login{
        String email;
        String password;
    }

    @Route
    public static String login(Transaction trans, @Body @Json Login login) throws SQLException, ClientError.Unauthorized, NoSuchAlgorithmException {
        int user_id;
        try(var stmt = trans.conn.namedPreparedStatement("select id from users where email=:email AND pass=:pass")){
            stmt.setString(":email", login.email);
            stmt.setString(":pass", login.password);
            var res = stmt.executeQuery();
            try{
                user_id = res.getInt(1);
            }catch (SQLException ignore){
                throw new ClientError.Unauthorized("An account with the specified email does not exist, or the specified password is incorrect");
            }
        }

        int session_id;
        try(var stmt = trans.conn.namedPreparedStatement("insert into sessions values(null, null, :user_id, :exp) returning id")){
            stmt.setInt(":user_id", user_id);
            stmt.setLong(":exp", new Date().getTime() + 2628000000L);
            session_id = stmt.executeQuery().getInt(1);
        }

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hash = digest.digest((login.email + "\0\0\0\0" + login.password + "\0\0\0\0" + session_id).getBytes());
        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            final String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        var token = String.format("%s%08X", hexString, session_id);

        try(var stmt = trans.conn.namedPreparedStatement("update sessions set token=:token where id=:id")){
            stmt.setString(":token", token);
            stmt.setInt(":id", session_id);
            stmt.execute();
        }

        return token;
    }

    @Route
    public static void invalidate_session(@FromRequest(UserAuthFromRequest.class) UserAuth auth, DbConnection conn, @Body String session_token) throws SQLException {
        try(var stmt = conn.namedPreparedStatement("delete from sessions where token=:token AND user_id=:id")){
            stmt.setString(":token", session_token);
            stmt.setInt(":id", auth.id);
            stmt.execute();
        }
    }

    public static class UserAuth{
        public int id;
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
                    System.out.println(token);
                    var result = stmt.executeQuery();
                    if(result==null||!result.next())throw new ClientError.Unauthorized("No valid session");

                    var auth = new UserAuth();
                    auth.id = result.getInt("id");
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
