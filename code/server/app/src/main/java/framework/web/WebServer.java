package framework.web;

import com.google.common.reflect.ClassPath;
import com.sun.net.httpserver.HttpContext;
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
import server.ServerStatistics;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class WebServer {
    protected final HttpServer server;
    /**
     * A store of all the state which is managed by this web server indexed by their associated type
     */
    private final HashMap<Class<?>, Object> managedState = new HashMap<>();
    private final InetSocketAddress address;

    /**
     * Constructor for WebServer.
     * @throws Exception for
     */
    public WebServer(InetSocketAddress address, int backlog) throws Exception {
        // on program exit try to do a graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        this.address = address;
        server = HttpServer.create(address, backlog);

        addManagedState(server); // Add HTTP Server to managed resources
    }

    public void start(){
        server.start();
        Logger.getGlobal().log(Level.INFO, "Server started on http://" + address.getAddress().getHostAddress() + ":" + address.getPort());
    }


    /**
     * Adds state to a pool of managed objects which are associated with this webserver.
     * The state is associated with a type and can be recalled through it.
     * </br></br>
     * State is shared with all routes and resources within the webserver, all state has the same scope.
     * All state unless the behavior is otherwise specified by the user or annotations can be recalled and used
     * by defining the state type in a @Route method parameter, or through {@link WebServer#getManagedState(Class)}
     *
     * @param state the state to be added
     * @param <T>   the type of state
     */
    public <T> void addManagedState(T state){
        managedState.put(state.getClass(), state);
    }

    /**
     * Adds state to a pool of managed objects which are associated with this webserver.
     * The state is associated with a type and can be recalled through it.
     * </br></br>
     * State is shared with all routes and resources within the webserver, all state has the same scope.
     * All state unless the behavior is otherwise specified by the user or annotations can be recalled and used
     * by defining the state type in a @Route method parameter, or through {@link WebServer#getManagedState(Class)}
     *
     * @param state the state to be added
     * @param clazz the super class for which this state will be associated to
     * @param <T> the type of state
     * @param <I> The type we want to associate this state with
     */
    public <T extends I, I> void addManagedState(T state, Class<I> clazz){
        managedState.put(clazz, state);
    }


    /**
     * retrieves managed state associated with the provided type.
     * @param clazz The class type of state
     * @return  The state itself can be null
     * @param <T> the type of state
     */
    @SuppressWarnings("unchecked")
    public <T> T getManagedState(Class<T> clazz){
        return (T) managedState.get(clazz);
    }

    /**
     * This method closes the webserver.
     */
    public void close(){
        Logger.getGlobal().log(Level.INFO, "Shutting down");
        for(var resource : managedState.values()){
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
    private static Stream<Class<?>> findAllClassesInPackage(String packageName) {
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
        try {
            for(var clazz : findAllClassesInPackage(classPath).toList()){
                var pack = clazz.getPackage().getName()+"/";
                var path = parentPath+pack.substring(classPath.length()+1).replace(".", "/");
                if(clazz.isAnnotationPresent(Routes.class)){
                    attachRoutes(builder, path, clazz);
                }else if(clazz.isAnnotationPresent(Handler.class)){
                    attachRouteHandler(path, (Class<? extends RequestHandler>) clazz);
                }
            }
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to load routes", e);
        }
    }

    private void attachRouteHandler(String parentPath, Class<? extends RequestHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var instance = handlerClass.getConstructor().newInstance();
        HttpHandler handler = exchange -> instance.handle(new Request(this, exchange, parentPath));
        attachHandler(parentPath, handler);
        Logger.getGlobal().log(Level.CONFIG, "Route mounted at: '" + parentPath + "' -> "+handlerClass);
    }

    private void attachRoutes(RequestsBuilder builder, String parentPath, Class<?> routeClass) {
        for(var method : routeClass.getDeclaredMethods()){
            if(method.getAnnotation(Route.class) == null) continue;
            var route = new RouteImpl(method, parentPath, builder);
            attachHandler(route.path, route.handler(this));

            Logger.getGlobal().log(Level.CONFIG, "Route mounted at: '" + route.path + "' -> "+route.sourceMethod);
        }
    }


    public HttpContext attachHandler(String path, HttpHandler handler){
        return server.createContext(path, handler);
    }
}
