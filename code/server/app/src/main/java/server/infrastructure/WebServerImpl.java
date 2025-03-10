package server.infrastructure;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import server.Config;
import server.Secrets;
import framework.db.DbManager;
import framework.web.TimedEvents;
import framework.web.WebServer;
import server.mail.MailServer;
import server.mail.SmtpMailServer;
import server.ServerStatistics;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Implementation of the web server for the Ticket Express application.
 */
public class WebServerImpl extends WebServer {

    public final ServerStatistics tracker;

    public WebServerImpl() throws Exception {
        super(new InetSocketAddress(Config.CONFIG.hostname, Config.CONFIG.port), Config.CONFIG.backlog);
        server.setExecutor(Executors.newFixedThreadPool(Config.CONFIG.web_threads));

        addManagedState(new TimedEvents());
        try{
            addManagedState(new DbManagerImpl());
            addManagedState(getManagedState(DbManagerImpl.class), DbManager.class);
        }catch (Exception e){
            this.close();
            throw e;
        }
        addManagedState(new SmtpMailServer(Secrets.get("email_account"), Secrets.get("email_password")), MailServer.class);

        tracker = new ServerStatistics(getManagedState(DbManager.class).getTracker());
        addManagedState(tracker);

        mount(new RequestBuilderImpl(), "/", "server.infrastructure.root");
    }

    @Override
    public HttpContext attachHandler(String path, HttpHandler handler){
        return super.attachHandler(path, exchange -> {
            var start = System.nanoTime();
            handler.handle(exchange);
            var code = exchange.getResponseCode();
            tracker.track_route(path, code, System.nanoTime()-start);
        });
    }
}
