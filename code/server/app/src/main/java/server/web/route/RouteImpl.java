package server.web.route;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.web.WebServer;
import server.web.annotations.http.Delete;
import server.web.annotations.http.Get;
import server.web.annotations.http.Post;
import server.web.annotations.http.Put;
import util.TypeReflect;
import util.func.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    final HttpHandler handler;
    public final int code;
    public final String method;

    protected RouteImpl(Method sourceMethod, String parentPath, RoutesBuilder builder) {
        this.sourceMethod = sourceMethod;

        var route = sourceMethod.getAnnotation(server.web.annotations.Route.class);
        this.code = route.code();
        var p = route.value().isEmpty()?"/"+sourceMethod.getName():route.value();
        this.pathParts = (parentPath+p).split("/");

        var path = "";
        for (String pathPart : this.pathParts) {
            if (pathPart.contains("<")) break;
            if (pathPart.isEmpty()) continue;
            path += "/"+pathPart;
        }
        path = path.isEmpty()?"/":path;
        this.path = path;

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

    private HttpHandler makeHandler() throws Throwable {
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

    public void sendResponse(Request request, byte[] content) throws IOException{
        sendResponse(request, code, content);
    }

    public void sendResponse(Request request, String content) throws IOException{
        sendResponse(request, code, content);
    }

    public <T> void sendResponse(Request request, T content) throws IOException{
        sendResponse(request, code, content);
    }

    public int findPathPartIndex(String value) {
        for(int i = 0; i < pathParts.length; i ++){
            if(pathParts[i].equals("<"+value+">")){
                return i;
            }
        }
        throw new RuntimeException("Cannot find path part '" + value + "' for " + sourceMethod);
    }

    public void sendResponse(Request request, int code, byte[] content) throws IOException {
        request.exchange.sendResponseHeaders(code, content.length);
        try (OutputStream os = request.exchange.getResponseBody()) {
            os.write(content);
        }
        Logger.getGlobal().logp(
            Level.FINE, sourceMethod.getDeclaringClass().getName(), sourceMethod.getName(),
            "Requested: '"+ path +"'" + " Response Code: "+code+" Content Length: "+content.length
        );
    }

    public void sendResponse(Request request, int code, String content) throws IOException{
        sendResponse(request, code, content.getBytes());
    }

    public <T> void sendResponse(Request request, int code, T message) throws IOException{
        sendResponse(request, code, new Gson().toJson(message));
    }

    public void addRoute(WebServer server) {
        var context = server.server.createContext(path, handler);
        context.getAttributes().put(WebServer.class.getName(), server);
    }

    public class Request{
        public final HttpExchange exchange;
        private Map<String, List<String>> queryMap;
        private String[] pathParts;

        public Request(HttpExchange exchange) {
            this.exchange = exchange;
        }

        public RouteImpl route(){
            return RouteImpl.this;
        }

        public void begin() throws ClientError.MethodNotAllowed {
            if(method != null && !method.equalsIgnoreCase(exchange.getRequestMethod()))
                throw new ClientError.MethodNotAllowed();
        }

        public List<String> getQueryParam(String param){
            return getQueryMap().get(param);
        }

        public boolean hasQueryParam(String param){
            return getQueryMap().get(param) != null;
        }

        public int countQueryParam(String param){
            var list = getQueryMap().get(param);
            return list==null?0:list.size();
        }

        public Map<String, List<String>> getQueryMap(){
            if(queryMap==null)queryMap = splitQuery();
            return queryMap;
        }

        private Map<String, List<String>> splitQuery() {
            var map = new HashMap<String, List<String>>();
            if(exchange.getRequestURI().getQuery()==null)return map;
            for(var element : exchange.getRequestURI().getQuery().split("[&;]")){
                var split = splitQueryParameter(element);
                var name = split[0];

                if (!map.containsKey(name))
                    map.put(name, new ArrayList<>());
                map.get(name).add(split.length==2?split[1]:null);
            }
            for(var entry : map.entrySet()){
                entry.setValue(ImmutableList.copyOf(entry.getValue()));
            }
            return ImmutableMap.copyOf(map);
        }

        private static String[] splitQueryParameter(String it) {
            var element = it.split("=", 2);
            for(int i = 0; i < element.length; i ++)
                element[0] = URLDecoder.decode(element[0], StandardCharsets.UTF_8);
            return element;
        }

        public int getPathSectionLen(){
            if(pathParts == null) {
                pathParts = exchange.getRequestURI().getPath().split("/");
                for(int i = 0; i < pathParts.length; i ++)
                    pathParts[0] = URLDecoder.decode(pathParts[0], StandardCharsets.UTF_8);
            }
            return pathParts.length;
        }

        public WebServer getServer(){
            return (WebServer) exchange.getHttpContext().getAttributes().get(WebServer.class.getName());
        }

        public String getPathPart(int index){
            getPathSectionLen();
            return pathParts[index];
        }
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
            sendResponse(request, 500, message + "\n" + ps);
        }catch (IOException e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to send server error message", e);
        }
    }

    private Request startRequest(HttpExchange exchange){
        return new Request(exchange);
    }

    @SuppressWarnings("unchecked")
    protected HttpHandler createRoute(){
        return exchange -> {
            Object[] r = new Object[phs.length];
            var request = startRequest(exchange);
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
    protected HttpHandler createRoute(Consume0 route){
        final var rh = (RouteReturn<Void>)ret;
        return exchange -> {
            var request = startRequest(exchange);
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
    protected <T1> HttpHandler createRoute(Consume1<T1> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        return exchange -> {
            T1 r1 = null;
            var request = startRequest(exchange);
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
    protected <T1, T2> HttpHandler createRoute(Consume2<T1, T2> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            var request = startRequest(exchange);
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
    protected <T1, T2, T3> HttpHandler createRoute(Consume3<T1, T2, T3> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            var request = startRequest(exchange);
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
    protected <T1, T2, T3, T4> HttpHandler createRoute(Consume4<T1, T2, T3, T4> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            var request = startRequest(exchange);
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
    protected <T1, T2, T3, T4, T5> HttpHandler createRoute(Consume5<T1, T2, T3, T4, T5> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            var request = startRequest(exchange);
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
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
    protected <T1, T2, T3, T4, T5, T6> HttpHandler createRoute(Consume6<T1, T2, T3, T4, T5, T6> route){
        final var rh = (RouteReturn<Void>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        final var r6_ph = (RouteParameter<T6>)phs[5];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            T6 r6 = null;
            var request = startRequest(exchange);
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
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
    protected <R> HttpHandler createRoute(Function0<R> route){
        final var rh = (RouteReturn<R>)ret;
        return exchange -> {
            var request = startRequest(exchange);
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
    protected <R, T1> HttpHandler createRoute(Function1<R, T1> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        return exchange -> {
            T1 r1 = null;
            var request = startRequest(exchange);
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
    protected <R, T1, T2> HttpHandler createRoute(Function2<R, T1, T2> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            var request = startRequest(exchange);
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
    protected <R, T1, T2, T3> HttpHandler createRoute(Function3<R, T1, T2, T3> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            var request = startRequest(exchange);
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
    protected <R, T1, T2, T3, T4> HttpHandler createRoute(Function4<R, T1, T2, T3, T4> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            var request = startRequest(exchange);
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
    protected <R, T1, T2, T3, T4, T5> HttpHandler createRoute(Function5<R, T1, T2, T3, T4, T5> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            var request = startRequest(exchange);
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
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
    protected <R, T1, T2, T3, T4, T5, T6> HttpHandler createRoute(Function6<R, T1, T2, T3, T4, T5, T6> route){
        final var rh = (RouteReturn<R>)ret;
        final var r1_ph = (RouteParameter<T1>)phs[0];
        final var r2_ph = (RouteParameter<T2>)phs[1];
        final var r3_ph = (RouteParameter<T3>)phs[2];
        final var r4_ph = (RouteParameter<T4>)phs[3];
        final var r5_ph = (RouteParameter<T5>)phs[4];
        final var r6_ph = (RouteParameter<T6>)phs[5];
        return exchange -> {
            T1 r1 = null;
            T2 r2 = null;
            T3 r3 = null;
            T4 r4 = null;
            T5 r5 = null;
            T6 r6 = null;
            var request = startRequest(exchange);
            try{
                request.begin();
                r1 = r1_ph.construct(request);
                r2 = r2_ph.construct(request);
                r3 = r3_ph.construct(request);
                r4 = r4_ph.construct(request);
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
