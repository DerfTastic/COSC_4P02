package server.web;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailServer implements Closeable {

    private final Session session;
    private final Transport transport;

    private final String username;

    private final Queue<Message> queue = new LinkedList<>();
    private volatile boolean open = true;

    public MailServer(String username, String password) throws MessagingException {
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

        transport = session.getTransport("smtp");
        transport.connect();

        new Thread(() -> {
            try {
                run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        Logger.getGlobal().log(Level.FINE, "Started Mail Service for '"+username+"'");
    }

    private void run() throws InterruptedException {
        while(open) {
            Message message;
            synchronized (queue) {
                while (queue.isEmpty()) {
                    queue.wait();
                    if (!open) return;
                }
                message = queue.poll();
            }
            try{
                if(!transport.isConnected())transport.connect();
                transport.sendMessage(message, message.getAllRecipients());
            }catch (Exception e){
                Logger.getGlobal().log(Level.WARNING, "Failed to send email message", e);
            }
        }
    }

    @Override
    public void close() {
        open = false;
        synchronized (queue){
            queue.notifyAll();
        }
        try {
            transport.close();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public interface MessageConfigurator{
        void config(Message message) throws MessagingException;
    }

    public static InternetAddress[] fromStrings(String... in) throws AddressException {
        var adds = new InternetAddress[in.length];
        for(int i = 0; i < adds.length; i ++){
            adds[i] = new InternetAddress(in[i]);
        }
        return adds;
    }

    public synchronized void sendMail(MessageConfigurator configurator) {
        try{
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            configurator.config(message);
            synchronized (queue){
                queue.add(message);
                queue.notify();
            }
        }catch (MessagingException e){
            throw new RuntimeException(e);
        }
    }
}
