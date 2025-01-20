package server.web;

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
import javax.mail.MessagingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class DbAPI {

    public static final class Person {
        public int id;
        public String name;
    }

    /*
            var content = """
                        <html>
            <body>
                <p>Greetings!</p>
                <div><img src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=http://localhost:8080"></div>
                <div><img src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Meow"></div>
                <div><img src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Nya"></div>
                <p>Salutations</p>
            </body>
        </html>
        """;
     */

    public static class Mail{
        String subject;
        String content;
        String[] to;
    }

    @Route
    public static void mail(MailServer server, @Body @Json Mail mail) throws MessagingException {
        server.sendMail(message -> {
            message.setRecipients(
                    Message.RecipientType.TO,
                    server.fromStrings(mail.to)
            );
            message.setSubject(mail.subject);
            message.setContent(mail.content, "text/html");
        });
    }

    @Route("/person/<id>")
    public static @Json Person person(Transaction trans, @Path int id) throws SQLException {
        try (var stmt = trans.conn.namedPreparedStatement("select * from person where id=:id")){
            stmt.setInt(":id",id);
            return SqlSerde.sqlSingle(stmt.executeQuery(), Person.class);
        }
    }

    @Route
    public static String sql(DbConnection connection, @Body String sql) throws SQLException {
        try(var stmt = connection.conn.createStatement()){
            if(!stmt.execute(sql))return "";
            String list = "";
            var rs = stmt.getResultSet();
            while(rs.next()){
                list += "(";
                for(int i = 1; ; i++){
                    try{
                        var res = rs.getString(i);
                        if(i!=1) list += ", ";
                        list += res;
                    }catch (SQLException ignore){break;}
                }
                list += ")\n";
            }
            return list;
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
        try(var stmt = trans.conn.namedPreparedStatement("insert into sessions values(null, null, :user_id) returning id")){
            stmt.setInt(":user_id", user_id);
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
    public static @Json UserAuth test(@FromRequest(UserAuthFromRequest.class) UserAuth auth){
        return auth;
    }

    public static class UserAuth{
        public int id;
        public String email;
        public String pass;

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
                try(var stmt = conn.namedPreparedStatement("select * from sessions left join users on sessions.user_id=users.id left join organizers on users.organizer_id=organizers.id where sessions.token=:token ")){
                    stmt.setString(":token", token);
                    System.out.println(token);
                    var result = stmt.executeQuery();
                    if(result==null||!result.next())throw new ClientError.Unauthorized("No valid session");

                    var auth = new UserAuth();
                    auth.id = result.getInt("id");
                    auth.email = result.getString("email");
                    auth.pass = result.getString("pass");

                    auth.organizer_id = result.getInt("organizer_id");
                    auth.max_events = result.getInt("max_events");
                    auth.has_analytics = result.getBoolean("has_analytics");
                    return auth;
                }
            }
        }
    }
}
