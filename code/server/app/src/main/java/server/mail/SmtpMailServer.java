package server.mail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SmtpMailServer implements Closeable, MailServer {
    private final Session session;

    private final String username;

    private final ExecutorService executor;

    private final ThreadLocal<Transport> transport;
    private final Pattern sender_filter;


    public SmtpMailServer(String username, String password, String sender_filter) {
        this.username = username;
        this.sender_filter = Pattern.compile(sender_filter);

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

        transport = ThreadLocal.withInitial(() -> {
            while(true){
                try{
                    var transport = session.getTransport("smtp");
                    transport.connect();
                    Logger.getGlobal().log(Level.FINE, "Initialized mail transport");
                    return transport;
                }catch (Exception e) {
                    Logger.getGlobal().log(Level.FINE, "Failed to initialize email transport", e);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        executor = Executors.newFixedThreadPool(32);
        Logger.getGlobal().log(Level.FINE, "Started Mail Service for '"+username+"'");
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public synchronized void sendMail(MessageConfigurator configurator) {
        executor.submit(() -> {
            try{
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username));
                configurator.config(message);
                for(var recip : message.getAllRecipients()) {
                    if (!sender_filter.matcher(recip.toString()).matches())
                        return;
                }
                transport.get().sendMessage(message, message.getAllRecipients());
                configurator.completed();
            }catch (MessagingException e){
                Logger.getGlobal().log(Level.WARNING, "Failed to send email", e);
            }
        });
    }
}
