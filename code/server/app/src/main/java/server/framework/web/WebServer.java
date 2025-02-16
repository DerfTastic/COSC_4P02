package server.framework.web;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.ServerLogger;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class WebServer {
    protected final HttpServer server; // Ticket Express server
    private final HashMap<Class<?>, Object> managedResources = new HashMap<>(); //
    public final ServerStatistics tracker = new ServerStatistics(); //
    private final InetSocketAddress address;

    /**
     * Constructor for WebServer.
     * @throws Exception for
     */
    public WebServer(InetSocketAddress address) throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close)); // ???
        this.address = address;
        server = HttpServer.create(address, 0); // Create HTTPServer at 'address' with no backlog

        addManagedResource(server); // Add HTTP Server to managed resources
        addManagedResource(tracker);
    }

    public void start(){
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

    public HttpContext createContext(String path, HttpHandler o) {
        return server.createContext(path, o);
    }
}
