package server.web;

import server.Secrets;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class MailServer {

    private final static Session session;

    private final static String username = Secrets.get("email_account");
    private final static String password = Secrets.get("email_password");
    static{

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        session = Session.getInstance(
                    prop,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
    }

    public static void sendEmail(String subject, String content, InternetAddress[] to) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(
                Message.RecipientType.TO,
                to
        );
        message.setSubject(subject);
        message.setContent(content, "text/html");

        Transport.send(message);
    }

    public static void main(String[] args) throws Exception {
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
        sendEmail("Purchased Tickets", content, InternetAddress.parse("pt21zs@brocku.ca"));

    }
}
