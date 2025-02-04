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

public class WebServer {
    public final HttpServer server;
    private final HashMap<Class<?>, Object> managedResources = new HashMap<>();
    public ServerStatistics tracker = new ServerStatistics();

    public WebServer() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        var address = new InetSocketAddress(Config.CONFIG.hostname, Config.CONFIG.port);
        server = HttpServer.create(address, 0);

        addManagedResource(server);
        addManagedResource(new TimedEvents());
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

    public <T> void addManagedResource(T resource){
        addManagedResource(resource.getClass(), resource);
    }

    public <I extends T, T> void addManagedResource(Class<I> clazz, T resource){
        managedResources.put(clazz, resource);
    }

    @SuppressWarnings("unchecked")
    public <T> T getManagedResource(Class<T> clazz){
        return (T) managedResources.get(clazz);
    }

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
