package server.infrastructure;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import server.Config;
import server.Secrets;
import framework.db.DbManager;
import framework.web.TimedEvents;
import framework.web.WebServer;
import framework.web.mail.MailServer;
import framework.web.mail.SmtpMailServer;
import server.ServerStatistics;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the web server for the Ticket Express application.
 */
public class WebServerImpl extends WebServer {

    public final ServerStatistics tracker;

    public WebServerImpl() throws Exception {
        // Create a socket address using "Config.CONFIG.hostname" as the respective hostname and
        // "Config.CONFIG.port" as the port.
        // Currently, localhost 80
        super(new InetSocketAddress(Config.CONFIG.hostname, Config.CONFIG.port));
        server.setExecutor(Executors.newFixedThreadPool(Config.CONFIG.web_threads));


        addManagedState(new TimedEvents()); // See timed events at main/java/server/web/route/TimedEvents
        addManagedState(new DynamicMediaHandler()); //
        try{
            addManagedState(new DbManager(Config.CONFIG.db_path, Config.CONFIG.store_db_in_memory, Config.CONFIG.wipe_db_on_start, true));
        }catch (Exception e){
            this.close();
            throw e;
        }


        {   // session expiration
            var db = getManagedState(DbManager.class);
            var timer = getManagedState(TimedEvents.class);
            timer.addMinutely(() -> {
                try(var trans = db.rw_transaction(">session_timer")){
                    try(var stmt = trans.namedPreparedStatement("delete from sessions where expiration<:now")){
                        stmt.setLong(":now", new Date().getTime());
                        stmt.execute();
                    }
                    trans.commit();
                    Logger.getGlobal().log(Level.FINE, "Ran session expiration clear");
                }catch (Exception e){
                    Logger.getGlobal().log(Level.SEVERE, "Failed to run session expiration clear", e);
                }
            });
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
