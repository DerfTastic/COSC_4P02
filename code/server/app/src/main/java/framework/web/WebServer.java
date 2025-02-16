package framework.web;

import com.google.common.reflect.ClassPath;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.ServerLogger;
import framework.web.request.Request;
import framework.web.request.RequestHandler;
import framework.web.annotations.Handler;
import framework.web.annotations.Route;
import framework.web.annotations.Routes;
import framework.web.route.RequestsBuilder;
import framework.web.route.RouteImpl;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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


    @SuppressWarnings("UnstableApiUsage")
    public static Stream<Class<?>> findAllClassesInPackage(String packageName) {
        try {
            return ClassPath.from(RequestsBuilder.class.getClassLoader())
                    .getTopLevelClassesRecursive(packageName)
                    .stream().map(ClassPath.ClassInfo::load);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void mount(RequestsBuilder builder, String parentPath, String classPath){
        try{
            for(var clazz : findAllClassesInPackage(classPath).toList()){
                var pack = clazz.getPackage().getName()+"/";
                var path = parentPath+pack.substring(classPath.length()+1).replace(".", "/");
                if(clazz.isAnnotationPresent(Routes.class)){
                    attachRoutes(builder, path, clazz);
                }else if(clazz.isAnnotationPresent(Handler.class)){
                    attachHandler(path, (Class<? extends RequestHandler>) clazz);
                }
            }
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to load routes", e);
        }
    }

    public void attachHandler(String parentPath, Class<? extends RequestHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var instance = handlerClass.getConstructor().newInstance();
        createHttpHandler(parentPath, -1, exchange -> {
            instance.handle(new Request(this, exchange) {
                @Override
                public String path() {
                    return parentPath;
                }
            });
        });
        Logger.getGlobal().log(Level.CONFIG, "Route mounted at: '" + parentPath + "' -> "+handlerClass);
    }

    public void attachRoutes(RequestsBuilder builder, String parentPath, Class<?> routeClass) {
        for(var method : routeClass.getDeclaredMethods()){
            if(method.getAnnotation(Route.class) == null) continue;
            var route = new RouteImpl(method, parentPath, builder);

            createHttpHandler(route.path, route.path.length(), route.handler(this));

            Logger.getGlobal().log(Level.CONFIG, "Route mounted at: '" + route.path + "' -> "+route.sourceMethod);
        }
    }

    private void createHttpHandler(String path, int pathCutoff, HttpHandler handler){
        server.createContext(path, exchange -> {
            var start = System.nanoTime();
            handler.handle(exchange);
            var duration = (System.nanoTime()-start)/1e9;

            var p = exchange.getRequestURI().getPath();
            if(pathCutoff>0)
                p = p.substring(0, pathCutoff);
            var code = exchange.getResponseCode();

            tracker.track_route(p, code, duration);
        });
    }
}
