package server.web.route;

import com.sun.net.httpserver.HttpServer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public abstract class RoutesBuilder {
    private final Class<?>[] routeClasses;

    public RoutesBuilder(Class<?>... routeClasses){
        this.routeClasses = routeClasses;
    }

    public void attachRoutes(HttpServer server, String parentPath) {
        for(var method : (Iterable<Method>)Arrays.stream(routeClasses).flatMap(aClass -> Arrays.stream(aClass.getDeclaredMethods()))::iterator){
            if(method.getAnnotation(server.web.annotations.Route.class) == null) continue;
            var route = new RouteImpl(method, parentPath, this);
            addRoute(server, route);
        }
    }

    protected void addRoute(HttpServer server, RouteImpl route){
        System.out.println(route.path);
        server.createContext(route.path, route.handler);
    }

    protected RouteParameter<?> getParameterHandler(RouteImpl route, Parameter param) throws Throwable{
        throw new RuntimeException("Unimplemented");
    }

    private StringsAdapter<?> getParameterStringAdapter(Class<?> type, boolean nullable) {
        throw new RuntimeException("Unimplemented");
    }

    private StringsAdapter<?> getParameterStringAdapter(Parameter param) {
        throw new RuntimeException("Unimplemented");
    }

    protected RouteReturn<?> getReturnHandler(RouteImpl route, Method method) throws Throwable{
        throw new RuntimeException("Unimplemented");
    }
}
