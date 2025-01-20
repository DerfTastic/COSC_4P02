package server.web;

import server.Secrets;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.function.Consumer;

public class MailServer {

    private final Session session;

    private final String username;
    public MailServer(String username, String password){
        this.username = username;

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

    public interface MessageConfigurator{
        void config(Message message) throws MessagingException;
    }

    public InternetAddress[] fromStrings(String... in) throws AddressException {
        var adds = new InternetAddress[in.length];
        for(int i = 0; i < adds.length; i ++){
            adds[i] = new InternetAddress(in[i]);
        }
        return adds;
    }

    public void sendMail(MessageConfigurator configurator) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        configurator.config(message);
        Transport.send(message);
    }
}
