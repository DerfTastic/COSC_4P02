package server.framework.web.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

public interface MessageConfigurator {
    void config(Message message) throws MessagingException;
}
