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

import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Implementation of the web server base for the Ticket Express application.
 */
public class WebServerImpl extends WebServer {

    public final ServerStatistics tracker;
    private final Config config;

    /**
     * Constructs and initializes the web server using the provided configuration.
     * @param config Runtime configuration for hostname, port, DB paths, etc.
     * @throws Exception Exception if database or server components fail during setup
     */
    public WebServerImpl(Config config) throws Exception {
        super(new InetSocketAddress(config.hostname, config.port), config.backlog);
        this.config = config;

        // Create a thread pool for WebServer to handle requests
        server.setExecutor(Executors.newFixedThreadPool(config.web_threads));

        addManagedState(config, Config.class);
        addManagedState(new TimedEvents());

        // Try to initialize the database manager and register it
        try{
            var db = new DbManagerImpl(config.db_path, config.store_db_in_memory, config.wipe_db_on_start, true);
            addManagedState(db, DbManager.class);
            addManagedState(db);
        }catch (Exception e){
            this.close();
            throw e;
        }

        var secrets = new Secrets(config.secrets_path);

        //
        MailServer mail;
        if(config.send_mail)
            mail = new SmtpMailServer(secrets.get("email_account"), secrets.get("email_password"), config.sender_filter);
        else
            mail = configurator -> {};
        addManagedState(mail, MailServer.class);

        // Instantiate and register server statistics tracker
        tracker = new ServerStatistics(getManagedState(DbManager.class).getTracker());
        addManagedState(tracker);

        // Mount the HTTP route handlers
        mount(new RequestBuilderImpl(config), "/", "server.infrastructure.root");
    }

    /**
     * Attaches an HTTP handler to an HTTP route.
     * @param path HTTP route.
     * @param handler Logic for processing the request.
     * @return HttpContext object representing the mounted handler.
     */
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

    /**
     *
     * @param p
     * @return
     */
    @Override
    protected Object getOnMountParameter(Parameter p) {
        if(p.isAnnotationPresent(server.infrastructure.param.Config.class)){
            var config = p.getAnnotation(server.infrastructure.param.Config.class);
            var name = config.name().equals("!")?p.getName():config.name();
            return this.config.get(name, p.getType());
        }
        return super.getOnMountParameter(p);
    }
}
