package server.web;

import com.sun.net.httpserver.HttpServer;
import server.Config;
import server.Secrets;
import server.db.DbManager;
import server.web.route.RoutesBuilder;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer {
    public final HttpServer server;
    private final HashMap<Class<?>, Object> managedResources = new HashMap<>();

    public WebServer() throws IOException, SQLException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        var address = new InetSocketAddress(Config.CONFIG.hostname, Config.CONFIG.port);
        server = HttpServer.create(address, 0);
        addManagedResource(server);
        try{
            addManagedResource(new DbManager());
        }catch (Exception e){
            this.close();
            throw e;
        }
        addManagedResource(new MailServer(Secrets.get("email_account"), Secrets.get("email_password")));

        server.createContext("/", new StaticContentHandler());
        new APIRouteBuilder(this).attachRoutes(this, "/api");
        new RoutesBuilder(MediaAPI.class).attachRoutes(this, "/media");

        server.setExecutor(Executors.newFixedThreadPool(Config.CONFIG.web_threads));
        server.start();

        Logger.getGlobal().log(Level.INFO, "Server started on http://" + address.getAddress().getHostAddress() + ":" + address.getPort());
    }

    public <T> void addManagedResource(T resource){
        managedResources.put(resource.getClass(), resource);
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
    }
}
