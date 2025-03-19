package server.infrastructure;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import server.Config;
import server.Secrets;
import framework.db.DbManager;
import framework.web.TimedEvents;
import framework.web.WebServer;
import server.mail.MailServer;
import server.mail.MessageConfigurator;
import server.mail.SmtpMailServer;
import server.ServerStatistics;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Implementation of the web server for the Ticket Express application.
 */
public class WebServerImpl extends WebServer {

    public final ServerStatistics tracker;

    public WebServerImpl(Config config) throws Exception {
        super(new InetSocketAddress(config.hostname, config.port), config.backlog);
        server.setExecutor(Executors.newFixedThreadPool(config.web_threads));

        addManagedState(config, Config.class);

        addManagedState(new TimedEvents());
        addManagedState(new FileDynamicMediaHandler(config), DynamicMediaHandler.class);
        try{
            var db = new DbManagerImpl(config);
            addManagedState(db, DbManager.class);
            addManagedState(db);
        }catch (Exception e){
            this.close();
            throw e;
        }
        var secrets = new Secrets(config);
        MailServer mail;
        if(config.send_mail)
            mail = new SmtpMailServer(secrets.get("email_account"), secrets.get("email_password"));
        else
            mail = configurator -> {};
        addManagedState(mail, MailServer.class);

        tracker = new ServerStatistics(getManagedState(DbManager.class).getTracker());
        addManagedState(tracker);

        mount(new RequestBuilderImpl(), "/", "server.infrastructure.root");
    }

    @Override
    public HttpContext attachHandler(String path, HttpHandler handler){
        return super.attachHandler(path, exchange -> {
            var start = System.nanoTime();
            var header = exchange.getResponseHeaders();
            header.add("Connection", "Keep-Alive");
            header.add("Keep-Alive", "timeout=14 max=100");
            handler.handle(exchange);
            var code = exchange.getResponseCode();
            tracker.track_route(path, code, System.nanoTime()-start);
        });
    }
}
