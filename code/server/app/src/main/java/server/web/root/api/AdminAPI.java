package server.web.root.api;

import server.ServerLogger;
import server.db.RoConn;
import server.db.RwConn;
import server.web.MailServer;
import server.web.ServerStatistics;
import server.web.annotations.*;
import server.web.annotations.url.Path;
import server.web.param.auth.RequireAdmin;
import server.web.param.auth.UserSession;
import server.web.route.ClientError;

import javax.mail.Message;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("unused")
@Routes
public class AdminAPI {

    public static class Mail{
        String subject;
        String content;
        String[] to;
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

    @Route
    public static void mail(@FromRequest(RequireAdmin.class) UserSession auth, MailServer server, @Body @Json Mail mail) {
        server.sendMail(message -> {
            message.setRecipients(
                    Message.RecipientType.TO,
                    MailServer.fromStrings(mail.to)
            );
            message.setSubject(mail.subject);
            message.setContent(mail.content, "text/html");
        });
    }

    @Route
    public static String execute_sql(@FromRequest(RequireAdmin.class) UserSession auth, RwConn connection, @Body String sql) throws SQLException {
        try(var stmt = connection.createStatement()){
            if(!stmt.execute(sql))return "";
            StringBuilder list = new StringBuilder();

            var rs = stmt.getResultSet();
            while(rs.next()){
                list.append("(");
                for(int i = 1; ; i++){
                    try{
                        var res = rs.getString(i);
                        if(i!=1) list.append(", ");
                        list.append(res);
                    }catch (SQLException ignore){break;}
                }
                list.append(")\n");
            }
            return list.toString();
        }
    }

    public record LogR(
            String level_s,
            int level_i,
            String message,
            long millis,
            long sequenceNumber,
            String sourceClassName,
            String sourceMethodName,
            String thrown
    ){}

    private static String formatException(Throwable t){
        if(t==null) return null;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    @Route
    public static @Json List<LogR> get_server_logs(@FromRequest(RequireAdmin.class) UserSession auth){

        return ServerLogger.streamify().map(r -> new LogR(
                r.getLevel().getName(),
                r.getLevel().intValue(),
                r.getMessage(),
                r.getMillis(),
                r.getSequenceNumber(),
                r.getSourceClassName(),
                r.getSourceMethodName(),
                formatException(r.getThrown())
        )).toList();
    }

    @Route("/delete_other_account/<email>")
    public static void delete_other_account(@FromRequest(RequireAdmin.class) UserSession auth, RoConn conn, @Path String email) throws SQLException, ClientError.BadRequest {
        try(var stmt = conn.namedPreparedStatement("delete from users where email=:email")){
            stmt.setString(":email", email);
            if(stmt.executeUpdate() != 1)
                throw new ClientError.BadRequest("Account with the specified email does not exist");
        }
    }

    @Route("/set_account_admin/<admin>/<email>")
    public static void set_account_admin(@FromRequest(RequireAdmin.class) UserSession auth, RoConn conn, @Path boolean admin, @Path String email) throws SQLException, ClientError.BadRequest {
        try(var stmt = conn.namedPreparedStatement("update users set admin=:admin where email=:email")){
            stmt.setString(":email", email);
            stmt.setBoolean(":admin", admin);
            if(stmt.executeUpdate() != 1)
                throw new ClientError.BadRequest("Account with the specified email does not exist");
        }
    }

    @Route
    public static String get_route_statistics(@FromRequest(RequireAdmin.class) UserSession auth, ServerStatistics tracker){
        return tracker.json();
    }
}
