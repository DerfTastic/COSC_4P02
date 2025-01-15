package server.web.route;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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

        return createRoute();
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
            if(method != null && method.equalsIgnoreCase(exchange.getRequestMethod()))
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
}
