package server.mail;


import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public interface MailServer {
    void sendMail(MessageConfigurator configurator);

    static InternetAddress[] fromStrings(String... in) throws AddressException {
        var adds = new InternetAddress[in.length];
        for(int i = 0; i < adds.length; i ++){
            adds[i] = new InternetAddress(in[i]);
        }
        return adds;
    }
}
