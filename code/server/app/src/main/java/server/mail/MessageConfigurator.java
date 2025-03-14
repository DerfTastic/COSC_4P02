package server.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

public interface MessageConfigurator {
    void config(Message message) throws MessagingException;
    default void completed() {};
}
