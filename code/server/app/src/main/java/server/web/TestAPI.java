package server.web;

import server.db.DbConnection;
import server.db.Transaction;
import server.web.annotations.Body;
import server.web.annotations.FromRequest;
import server.web.annotations.Json;
import server.web.annotations.Route;
import server.web.annotations.url.Path;
import util.SqlSerde;

import javax.mail.Message;
import java.sql.SQLException;


@SuppressWarnings("unused")
public class TestAPI {


    @Route
    public static @Json AccountAPI.UserAuth test(@FromRequest(AccountAPI.UserAuthFromRequest.class) AccountAPI.UserAuth auth){
        return auth;
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
    public static void mail(MailServer server, @Body @Json Mail mail) {
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
}
