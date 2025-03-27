package framework.web;

import com.google.common.reflect.ClassPath;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import framework.web.annotations.OnMount;
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
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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


    public WebServer(InetSocketAddress address, int backlog) throws Exception {
        this(HttpServer.create(address, backlog));
    }

    public WebServer(HttpServer server){
        this.address = server.getAddress();
        this.server = server;

        addManagedState(server, HttpServer.class); // Add HTTP Server to managed resources
        addManagedState(this, WebServer.class); // Add HTTP Server to managed resources
        addManagedState(this); // Add HTTP Server to managed resources

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
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
                    .getAllClasses()
                    .stream()
                    .filter(classInfo -> classInfo.getPackageName().startsWith(packageName))
                    .map(ClassPath.ClassInfo::load);
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
            throw new RuntimeException(e);
        }
    }

    private void attachRouteHandler(String parentPath, Class<? extends RequestHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var constructor = handlerClass.getConstructors()[0];
        var paramsTy = constructor.getParameters();
        var params  = new Object[paramsTy.length];
        for(int p = 0; p < params.length; p ++){
            params[p] = getOnMountParameter(paramsTy[p]);
        }

        var instance = (RequestHandler)constructor.newInstance(params);
        HttpHandler handler = exchange -> instance.handle(new Request(this, exchange, parentPath));
        attachHandler(parentPath, handler);
        Logger.getGlobal().log(Level.CONFIG, "Route mounted at: '" + parentPath + "' -> "+handlerClass);
        onMount(handlerClass, instance);
    }

    private void attachRoutes(RequestsBuilder builder, String parentPath, Class<?> routeClass) {
        for(var method : routeClass.getDeclaredMethods()){
            if(method.getAnnotation(Route.class) == null) continue;
            var route = new RouteImpl(method, parentPath, builder);
            attachHandler(route.path, route.handler(this));

            Logger.getGlobal().log(Level.CONFIG, "Route mounted at: '" + route.path + "' -> "+route.sourceMethod);
        }
        onMount(routeClass, null);
    }

    private void onMount(Class<?> clazz, Object instance){
        for(var method : clazz.getDeclaredMethods()){
            if(!method.isAnnotationPresent(OnMount.class))
                continue;
            if(!Modifier.isStatic(method.getModifiers()) && instance==null){
                Logger.getGlobal().log(Level.SEVERE, "On mount declared non static, but no instance available " + method);
                return;
            }
            var paramTypes = method.getParameters();
            var params = new Object[paramTypes.length];
            for(int i = 0; i < paramTypes.length; i ++){
                params[i] = getOnMountParameter(paramTypes[i]);
            }
            try {
                method.invoke(instance, params);
                Logger.getGlobal().log(Level.CONFIG, "Completed on mount " + method);
            } catch (Exception e) {
                Logger.getGlobal().log(Level.SEVERE, "Failed to run on mount " + method);
                throw new RuntimeException(e);
            }
        }
    }

    protected Object getOnMountParameter(Parameter p){
        return getManagedState(p.getType());
    }

    public HttpContext attachHandler(String path, HttpHandler handler){
        return server.createContext(path, handler);
    }
}
