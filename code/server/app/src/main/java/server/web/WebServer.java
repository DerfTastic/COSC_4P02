package server.web;

import com.sun.net.httpserver.HttpServer;
import server.Config;
import server.DynamicMediaHandler;
import server.Secrets;
import server.ServerLogger;
import server.db.DbManager;
import server.web.mail.MailServer;
import server.web.mail.SmtpMailServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class WebServer {
    public final HttpServer server; // Ticket Express server
    private final HashMap<Class<?>, Object> managedResources = new HashMap<>(); //
    public ServerStatistics tracker = new ServerStatistics(); //

    /**
     * Constructor for WebServer.
     * @throws Exception for
     */
    public WebServer() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close)); // ???

        // Create a socket address using "Config.CONFIG.hostname" as the respective hostname and
        // "Config.CONFIG.port" as the port.
        // Currently, localhost 80
        var address = new InetSocketAddress(Config.CONFIG.hostname, Config.CONFIG.port);
        server = HttpServer.create(address, 0); // Create HTTPServer at 'address' with no backlog

        addManagedResource(server); // Add HTTP Server to managed resources
        addManagedResource(new TimedEvents()); // See timed events at main/java/server/web/route/TimedEvents
        addManagedResource(tracker);
        addManagedResource(new DynamicMediaHandler());
        try{
            addManagedResource(new DbManager());
        }catch (Exception e){
            this.close();
            throw e;
        }


        {   // session expiration
            var db = getManagedResource(DbManager.class);
            db.setStatsTracker(tracker);
            var timer = getManagedResource(TimedEvents.class);
            timer.addMinutely(() -> {
                try(var trans = db.rw_transaction()){
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

        addManagedResource(MailServer.class, new SmtpMailServer(Secrets.get("email_account"), Secrets.get("email_password")));

        new APIRouteBuilder(this).mountRoutes(this, "/", "server.web.root");

        server.setExecutor(Executors.newFixedThreadPool(Config.CONFIG.web_threads));
        server.start();

        Logger.getGlobal().log(Level.INFO, "Server started on http://" + address.getAddress().getHostAddress() + ":" + address.getPort());
    }

    /**
     * This method adds a resource object of type T to managedResources by calling the function directly below.
     * See function below.
     * @param resource
     * @param <T>
     */
    public <T> void addManagedResource(T resource){
        // Deconstruct resource into
        addManagedResource(resource.getClass(), resource);
    }

    /**
     *
     * @param clazz
     * @param resource
     * @param <I>
     * @param <T>
     */
    public <I extends T, T> void addManagedResource(Class<I> clazz, T resource){
        managedResources.put(clazz, resource);
    }

    /**
     *  This method returns a managed resource when provided a class.
     * @param clazz The class object being retrieved from 'managedResources'
     * @return Returns a managed resource retrieved using the provided class
     * @param <T> Generic Class Template
     */
    @SuppressWarnings("unchecked")
    public <T> T getManagedResource(Class<T> clazz){
        return (T) managedResources.get(clazz);
    }

    /**
     * This method closes the webserver.
     */
    public void close(){
        Logger.getGlobal().log(Level.INFO, "Shutting down");
        for(var resource : managedResources.values()){
            if(resource instanceof Closeable c){
                try{
                    c.close();
                }catch (Exception e){
                    Logger.getGlobal().log(Level.SEVERE, "Failed to close resource", e);
                }
            }
        }
        Logger.getGlobal().log(Level.INFO, "Shutdown complete");
        ServerLogger.close();
    }
}
