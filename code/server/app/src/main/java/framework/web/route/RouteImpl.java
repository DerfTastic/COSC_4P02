package framework.web.route;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import framework.util.func.*;
import server.framework.util.func.*;
import framework.web.request.Request;
import framework.web.request.RequestHandler;
import framework.web.WebServer;
import framework.web.annotations.Route;
import framework.web.annotations.http.Delete;
import framework.web.annotations.http.Get;
import framework.web.annotations.http.Post;
import framework.web.annotations.http.Put;
import framework.web.error.ClientError;
import framework.web.error.MethodNotAllowed;
import framework.util.TypeReflect;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RouteImpl {
    final static Class<?>[] consumers = new Class[]{Consume0.class, Consume1.class, Consume2.class, Consume3.class, Consume4.class, Consume5.class, Consume6.class};
    final static Class<?>[] functions = new Class[]{Function0.class, Function1.class, Function2.class, Function3.class, Function4.class, Function5.class, Function6.class};

    public final Method sourceMethod;
    public final String path;
    private final String[] pathParts;
    final RouteParameter<?>[] phs;
    final RouteReturn<?> ret;
    public final RequestHandler handler;
    public final int code;
    public final String method;

    public RouteImpl(Method sourceMethod, String parentPath, RequestsBuilder builder) {
        this.sourceMethod = sourceMethod;

        var route = sourceMethod.getAnnotation(Route.class);
        this.code = route.code();
        var p = route.value().isEmpty()?sourceMethod.getName():route.value().substring(1);
        this.pathParts = (parentPath+p).split("/");

        StringBuilder path = new StringBuilder();
        for (String pathPart : this.pathParts) {
            if (pathPart.contains("<")) break;
            if (pathPart.isEmpty()) continue;
            path.append("/").append(pathPart);
        }
        path = new StringBuilder((path.isEmpty()) ? "/" : path.toString());
        this.path = path.toString();

        if(sourceMethod.getAnnotation(Get.class) != null){
            this.method = "GET";
        }else if(sourceMethod.getAnnotation(Post.class) != null){
            this.method = "POST";
        }else if(sourceMethod.getAnnotation(Delete.class) != null){
            this.method = "DELETE";
        }else if(sourceMethod.getAnnotation(Put.class) != null){
            this.method = "PUT";
        }else{
            this.method = null;
        }

        if(!Modifier.isStatic(sourceMethod.getModifiers()))
            throw new RuntimeException("Route method '" + sourceMethod.getName() + "' needs to be static!");

        var params = sourceMethod.getParameters();
        phs = new RouteParameter[params.length];

        try{
            ret = builder.getReturnHandler(this, sourceMethod);

            for(int i = 0; i < phs.length; i ++) {
                phs[i] = builder.getParameterHandler(this, params[i]);
            }

            this.handler = makeHandler();
        }catch (Throwable e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to construct API route", e);
            throw new RuntimeException(e);
        }
    }

    private RequestHandler makeHandler() throws Throwable {
        var params = sourceMethod.getParameters();

        // fallback(slower)
        if(sourceMethod.getParameters().length > 6){
            return createRoute();
        }

        var caller = MethodHandles.lookup();
        var implementation = caller.findStatic(sourceMethod.getDeclaringClass(), sourceMethod.getName(), MethodType.methodType(sourceMethod.getReturnType(), sourceMethod.getParameterTypes()));

        boolean isVoid = sourceMethod.getReturnType().equals(void.class);
        var factoryType = MethodType.methodType(isVoid?consumers[params.length]:functions[params.length]);
        var interfaceMethodName = "call";

        Class<?>[] args = new Class<?>[params.length];
        Arrays.fill(args, Object.class);
        var interfaceMethodType = MethodType.methodType(isVoid?void.class:Object.class, args);
        var types = sourceMethod.getParameterTypes();
        for(int i = 0; i < types.length; i ++){
            if(types[i].isPrimitive()) types[i] = TypeReflect.wrapPrimitives(types[i]);
        }
        var dynamicMethodType = MethodType.methodType(isVoid?void.class:Object.class, types);

        var factory = LambdaMetafactory.metafactory(
                caller,
                interfaceMethodName,
                factoryType,
                interfaceMethodType,
                implementation,
                dynamicMethodType
        );

        var lamda = factory.getTarget().invoke();
        return switch((isVoid?0:1000)+params.length){
            case 0 -> createRoute((Consume0)lamda);
            case 1 -> createRoute((Consume1<?>)lamda);
            case 2 -> createRoute((Consume2<?, ?>)lamda);
            case 3 -> createRoute((Consume3<?, ?, ?>)lamda);
            case 4 -> createRoute((Consume4<?, ?, ?, ?>)lamda);
            case 5 -> createRoute((Consume5<?, ?, ?, ?, ?>)lamda);
            case 6 -> createRoute((Consume6<?, ?, ?, ?, ?, ?>)lamda);

            case 1000 -> createRoute((Function0<?>)lamda);
            case 1001 -> createRoute((Function1<?, ?>)lamda);
            case 1002 -> createRoute((Function2<?, ?, ?>)lamda);
            case 1003 -> createRoute((Function3<?, ?, ?, ?>)lamda);
            case 1004 -> createRoute((Function4<?, ?, ?, ?, ?>)lamda);
            case 1005 -> createRoute((Function5<?, ?, ?, ?, ?, ?>)lamda);
            case 1006 -> createRoute((Function6<?, ?, ?, ?, ?, ?, ?>)lamda);
            default -> throw new RuntimeException("Invalid State");
        };
    }

    public int findPathPartIndex(String value) {
        for(int i = 0; i < pathParts.length; i ++){
            if(pathParts[i].equals("<"+value+">")){
                return i;
            }
        }
        throw new RuntimeException("Cannot find path part '" + value + "' for " + sourceMethod);
    }

    private void parameterDestructError(int param, Throwable e){
        Logger.getGlobal().logp(Level.SEVERE,
                sourceMethod.getDeclaringClass().getName(),
                sourceMethod.getName(),
                "Exception destructing parameter("+param+") "+ sourceMethod.getParameters()[param]+" for route'" + path + "'" + param,
                e
        );
    }

    private void parameterDestructOnErrorError(int param, Throwable e){
        parameterDestructError(param, e);
    }

    private void routeError(Request request, Throwable e){
        Logger.getGlobal().logp(Level.SEVERE,
                sourceMethod.getDeclaringClass().getName(),
                sourceMethod.getName(),
                "Error while servicing route '" + path + "'",
                e
        );
        try{
            sendServerError(request, "Server failed to complete request", e);
        }catch (Exception ignore){}
    }

    public void sendServerError(Request request, String message, Throwable exception) {
        try{
            var ps = new StringWriter();
            exception.printStackTrace(new PrintWriter(ps));
            request.sendResponse(500, message + "\n" + ps);
        }catch (IOException e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to send server error message", e);
        }
    }

    public HttpHandler handler(WebServer server){
        return exchange -> {
            this.handler.handle(new RequestImpl(server, exchange));
        };
    }

    private class RequestImpl extends Request{

        @Override
        public void begin() throws MethodNotAllowed {
            if (method != null && !method.equalsIgnoreCase(exchange.getRequestMethod()))
                throw new MethodNotAllowed();
        }

        public RequestImpl(WebServer server, HttpExchange exchange) {
            super(server, exchange);
        }

        @Override
        protected void logResponse(String path, int code, int len) {
            Logger.getGlobal().logp(
                    Level.FINE, sourceMethod.getDeclaringClass().getName(), sourceMethod.getName(),
                    "Requested: '"+ path +"'" + " Response Code: "+code+" Content Length: "+len
            );
        }

        @Override
        public int code() {
            return RouteImpl.this.code;
        }

        @Override
        public String path() {
            return RouteImpl.this.path;
        }
    }

    @SuppressWarnings("unchecked")
    protected RequestHandler createRoute(){
        return request -> {
            Object[] r = new Object[phs.length];
            try{
                request.begin();
                for(int i = 0; i < phs.length; i ++)
                    r[i] = phs[i].construct(request);
                var response = sourceMethod.invoke(null, r);
                ((RouteReturn<Object>)ret).send(request, response);
            }catch (Throwable re){
                for(int i = 0; i < phs.length; i ++)
                    try{
                        if(r[i]!=null)
                            ((RouteParameter<Object>)phs[i]).destructError(request, r[i]);
                    }catch (Exception e){
                        parameterDestructOnErrorError(i, e);
                    }
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            for(int i = 0; i < phs.length; i ++)
                try{
                    if(r[i]!=null)((RouteParameter<Object>)phs[i]).destruct(request, r[i]);
                }catch (Exception e){
                    parameterDestructOnErrorError(i, e);
                }
        };
    }

    @SuppressWarnings("unchecked")
    protected RequestHandler createRoute(Consume0 route){
        final var rh = (RouteReturn<Void>)ret;
        return request -> {
            try{
                request.begin();
                route.call();
                rh.send(request, null);
            }catch (Exception re){
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <T1> RequestHandler createRoute(Consume1<T1> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        return request -> {
            T1 r1 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                route.call(r1);
                rh.send(request, null);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2> RequestHandler createRoute(Consume2<T1, T2> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                route.call(r1, r2);
                rh.send(request, null);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3> RequestHandler createRoute(Consume3<T1, T2, T3> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                route.call(r1, r2, r3);
                rh.send(request, null);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4> RequestHandler createRoute(Consume4<T1, T2, T3, T4> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
                route.call(r1, r2, r3, r4);
                rh.send(request, null);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                try{if(r4!=null)r4_ph.destructError(request, r4);}catch (Exception e){parameterDestructOnErrorError(4, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
            try{r4_ph.destruct(request, r4);}catch (Exception e){parameterDestructError(4, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5> RequestHandler createRoute(Consume5<T1, T2, T3, T4, T5> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
                r5 = r5_ph.construct(request);
                route.call(r1, r2, r3, r4, r5);
                rh.send(request, null);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                try{if(r4!=null)r4_ph.destructError(request, r4);}catch (Exception e){parameterDestructOnErrorError(4, e);}
                try{if(r5!=null)r5_ph.destructError(request, r5);}catch (Exception e){parameterDestructOnErrorError(5, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
            try{r4_ph.destruct(request, r4);}catch (Exception e){parameterDestructError(4, e);}
            try{r5_ph.destruct(request, r5);}catch (Exception e){parameterDestructError(5, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <T1, T2, T3, T4, T5, T6> RequestHandler createRoute(Consume6<T1, T2, T3, T4, T5, T6> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        final var r6_ph = (RouteParameter<T6>)phs[5];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            T6 r6 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
                r5 = r5_ph.construct(request);
                r6 = r6_ph.construct(request);
                route.call(r1, r2, r3, r4, r5, r6);
                rh.send(request, null);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                try{if(r4!=null)r4_ph.destructError(request, r4);}catch (Exception e){parameterDestructOnErrorError(4, e);}
                try{if(r5!=null)r5_ph.destructError(request, r5);}catch (Exception e){parameterDestructOnErrorError(5, e);}
                try{if(r6!=null)r6_ph.destructError(request, r6);}catch (Exception e){parameterDestructOnErrorError(6, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
            try{r4_ph.destruct(request, r4);}catch (Exception e){parameterDestructError(4, e);}
            try{r5_ph.destruct(request, r5);}catch (Exception e){parameterDestructError(5, e);}
            try{r6_ph.destruct(request, r6);}catch (Exception e){parameterDestructError(6, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <R> RequestHandler createRoute(Function0<R> route){
        final var rh = (RouteReturn<R>)ret;
        return request -> {
            try{
                request.begin();
                var result = route.call();
                rh.send(request, result);
            }catch (Exception re){
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <R, T1> RequestHandler createRoute(Function1<R, T1> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        return request -> {
            T1 r1 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                var result = route.call(r1);
                rh.send(request, result);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <R, T1, T2> RequestHandler createRoute(Function2<R, T1, T2> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                var result = route.call(r1, r2);
                rh.send(request, result);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <R, T1, T2, T3> RequestHandler createRoute(Function3<R, T1, T2, T3> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                var result = route.call(r1, r2, r3);
                rh.send(request, result);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <R, T1, T2, T3, T4> RequestHandler createRoute(Function4<R, T1, T2, T3, T4> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
                var result = route.call(r1, r2, r3, r4);
                rh.send(request, result);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                try{if(r4!=null)r4_ph.destructError(request, r4);}catch (Exception e){parameterDestructOnErrorError(4, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
            try{r4_ph.destruct(request, r4);}catch (Exception e){parameterDestructError(4, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <R, T1, T2, T3, T4, T5> RequestHandler createRoute(Function5<R, T1, T2, T3, T4, T5> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
                r5 = r5_ph.construct(request);
                var result = route.call(r1, r2, r3, r4, r5);
                rh.send(request, result);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                try{if(r4!=null)r4_ph.destructError(request, r4);}catch (Exception e){parameterDestructOnErrorError(4, e);}
                try{if(r5!=null)r5_ph.destructError(request, r5);}catch (Exception e){parameterDestructOnErrorError(5, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
            try{r4_ph.destruct(request, r4);}catch (Exception e){parameterDestructError(4, e);}
            try{r5_ph.destruct(request, r5);}catch (Exception e){parameterDestructError(5, e);}
        };
    }

    @SuppressWarnings("unchecked")
    protected <R, T1, T2, T3, T4, T5, T6> RequestHandler createRoute(Function6<R, T1, T2, T3, T4, T5, T6> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        final var r6_ph = (RouteParameter<T6>)phs[5];
        return request -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            T6 r6 = null;
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
                r5 = r5_ph.construct(request);
                r6 = r6_ph.construct(request);
                var result = route.call(r1, r2, r3, r4, r5, r6);
                rh.send(request, result);
            }catch (Exception re){
                try{if(r1!=null)r1_ph.destructError(request, r1);}catch (Exception e){parameterDestructOnErrorError(1, e);}
                try{if(r2!=null)r2_ph.destructError(request, r2);}catch (Exception e){parameterDestructOnErrorError(2, e);}
                try{if(r3!=null)r3_ph.destructError(request, r3);}catch (Exception e){parameterDestructOnErrorError(3, e);}
                try{if(r4!=null)r4_ph.destructError(request, r4);}catch (Exception e){parameterDestructOnErrorError(4, e);}
                try{if(r5!=null)r5_ph.destructError(request, r5);}catch (Exception e){parameterDestructOnErrorError(5, e);}
                try{if(r6!=null)r6_ph.destructError(request, r6);}catch (Exception e){parameterDestructOnErrorError(6, e);}
                if(re instanceof ClientError e)
                    e.respond(request);
                else
                    routeError(request, re);
                return;
            }
            try{r1_ph.destruct(request, r1);}catch (Exception e){parameterDestructError(1, e);}
            try{r2_ph.destruct(request, r2);}catch (Exception e){parameterDestructError(2, e);}
            try{r3_ph.destruct(request, r3);}catch (Exception e){parameterDestructError(3, e);}
            try{r4_ph.destruct(request, r4);}catch (Exception e){parameterDestructError(4, e);}
            try{r5_ph.destruct(request, r5);}catch (Exception e){parameterDestructError(5, e);}
            try{r6_ph.destruct(request, r6);}catch (Exception e){parameterDestructError(6, e);}
        };
    }
}
