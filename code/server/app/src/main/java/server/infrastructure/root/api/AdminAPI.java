package server.infrastructure.root.api;

import framework.db.RwTransaction;
import framework.web.annotations.*;
import server.ServerLogger;
import framework.db.RwConn;
import framework.web.error.BadRequest;
import server.infrastructure.session.AdminSession;
import server.mail.MailServer;
import server.ServerStatistics;
import framework.web.annotations.url.Path;

import javax.mail.Message;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * A group of HTTP API endpoints that provides important admin functions such as sending out emails to users, managing logs, getting server statistics, and more.
 */
@SuppressWarnings("unused")
@Routes
public class AdminAPI {

    /**
     * Represents all necessary parts of an email communication,
     * including its {@link Mail#to recipients}, the {@link Mail#subject subject},
     * and the actual {@link Mail#content content} of the email itself
     */
    public static class Mail{
        String subject;
        String content;
        String[] to;

        public Mail(String subject, String content, String[] to) {
            this.subject = subject;
            this.content = content;
            this.to = to;
        }
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

    /**
     * Sends an email.
     *
     * @param auth An {@link AdminSession} to do this with
     * @param server The {@link MailServer} used to send them mail
     * @param mail The actual email content, subject, and recipients (see {@link Mail})
     */
    @Route
    public static void mail(AdminSession auth, MailServer server, @Body @Json Mail mail) {
        server.sendMail(message -> {
            message.setRecipients(
                    Message.RecipientType.TO,
                    MailServer.fromStrings(mail.to)
            );
            message.setSubject(mail.subject);
            message.setContent(mail.content, "text/html");
        });
    }

    /**
     * Executes SQL statements
     * @param auth An {@link AdminSession} to do this with
     * @param connection A Read/write connection
     * @param sql The SQL code string
     * @return The output/result of the executed sql. Will say how many rows were updated with "Updated <i>X</i>" at the beginning of the string.
     * @throws SQLException
     */
    @Route
    public static String execute_sql(AdminSession auth, RwConn connection, @Body String sql) throws SQLException {
        StringBuilder list;
        try (var stmt = connection.createStatement()) {
            var rows = stmt.execute(sql);
            list = new StringBuilder();

            var rs = stmt.getResultSet();
            list.append("Updated ").append(stmt.getUpdateCount()).append("\n");
            if (rs == null) return list.toString();
            if (!rows) return list.toString();
            while (rs.next()) {
                list.append("(");
                for (int i = 1; ; i++) {
                    try {
                        var res = rs.getString(i);
                        if (i != 1) list.append(", ");
                        list.append(res);
                    } catch (SQLException ignore) {
                        break;
                    }
                }
                list.append(")\n");
            }
        }
        connection.commit();
        return list.toString();
    }

    /** Represents a log record */
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

    /** Gets server logs from {@link ServerLogger}
     * @param auth The {@link AdminSession} that's getting the logs
     */
    @Route
    public static @Json List<LogR> get_server_logs(AdminSession auth){

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
    public static void delete_other_account(AdminSession auth, RwTransaction trans, @Path String email) throws SQLException, BadRequest {
        try(var stmt = trans.namedPreparedStatement("delete from users where email=:email")){
            stmt.setString(":email", email);
            if(stmt.executeUpdate() != 1)
                throw new BadRequest("Account with the specified email does not exist");
        }
        trans.commit();
    }

    /**
     * Sets the admin flag for a given user.
     * @param auth The AdminSession that's doing this
     * @param trans The transaction this is happening in
     * @param admin Whether the user in question is going to be admin or not (admin flag)
     * @param email The email of user who we are desiring to set the admin flag for
     * @throws SQLException If SQL throws an exception
     * @throws BadRequest If an account with the specified email does not exist
     */
    @Route("/set_account_admin/<admin>/<email>")
    public static void set_account_admin(AdminSession auth, RwTransaction trans, @Path boolean admin, @Path String email) throws SQLException, BadRequest {
        long id;
        try(var stmt = trans.namedPreparedStatement("update users set admin=:admin where email=:email returning id")){
            stmt.setString(":email", email);
            stmt.setBoolean(":admin", admin);
            var rs = stmt.executeQuery();
            if(!rs.next())
                throw new BadRequest("Account with the specified email does not exist");
            id = rs.getLong("id");
        }
        try(var stmt = trans.namedPreparedStatement("delete from sessions where user_id=:id")){
            stmt.setLong(":id", id);
            stmt.execute();
        }
        trans.commit();
    }

    /**
     * @return the server statistics (used memory and system time) as a JSON string, as an array of bytes
     * @param auth The {@link AdminSession} that's doing this
     * @param tracker
     */
    @Route
    public static byte[] get_server_statistics(AdminSession auth, ServerStatistics tracker){
        return tracker.json();
    }

    /**
     * Sets the current log level of {@link ServerLogger}
     * @param auth The {@link AdminSession} that's doing this
     * @param level The Log level to set {@link ServerLogger} to
     */
    @Route("/set_log_level/<level>")
    public static void set_log_level(AdminSession auth, @Path String level){
        ServerLogger.setLogLevel(Level.parse(level));
    }

    /**
     * @return The current log level from {@link ServerLogger}
     * @param auth The {@link AdminSession} that's doing this
     */
    @Route
    public static String get_log_level(AdminSession auth){
        return ServerLogger.getLogLevel().getName();
    }

    /**
     * @return The various log levels in {@link java.util.logging.Level}
     * @param auth The {@link AdminSession} that's doing this
     */
    @Route
    public static @Json String[] get_log_levels(AdminSession auth){
        return new String[]{
                Level.OFF.getName(), Level.SEVERE.getName(), Level.WARNING.getName(), Level.INFO.getName(), Level.CONFIG.getName(), Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName(), Level.ALL.getName()
        };
    }
}
