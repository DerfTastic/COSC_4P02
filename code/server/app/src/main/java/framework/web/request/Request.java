package framework.web.request;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import framework.web.WebServer;
import framework.web.error.ClientError;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Request {
    public final HttpExchange exchange;
    public final WebServer server;
    private Map<String, List<String>> queryMap;
    private String[] pathParts;

    public Request(WebServer server, HttpExchange exchange) {
        this.server = server;
        this.exchange = exchange;
    }

    public void begin() throws ClientError {}

    public int code(){
        return 200;
    }

    protected void logResponse(String path, int code, int len){
        var frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(
                        e -> e
                                .skip(1)
                                .filter(f ->
                                        !f.getMethodName().equals("sendResponse")
                                )
                                .findFirst()
                ).get();
        Logger.getGlobal().logp(
                Level.FINE, frame.getClassName(), frame.getMethodName(),
                "Requested: '"+path+"'" + " Response Code: "+code+" Content Length: "+len
        );
    }

    public void sendResponse(int code, byte[] content) throws IOException{
        exchange.sendResponseHeaders(code, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
        logResponse(exchange.getRequestURI().toString(), code, content.length);
    }

    public void sendResponse(byte[] content) throws IOException{
        sendResponse(code(), content);
    }

    public void sendResponse(String content) throws IOException{
        sendResponse(code(), content);
    }

    public <T> void sendResponse(T content) throws IOException{
        sendResponse(code(), content);
    }

    public void sendResponse(int code, String content) throws IOException{
        sendResponse(code, content.getBytes());
    }

    public <T> void sendResponse(int code, T message) throws IOException{
        sendResponse(code, new Gson().toJson(message));
    }


    public List<String> getQueryParam(String param) {
        return getQueryMap().get(param);
    }

    public boolean hasQueryParam(String param) {
        return getQueryMap().get(param) != null;
    }

    public int countQueryParam(String param) {
        var list = getQueryMap().get(param);
        return list == null ? 0 : list.size();
    }

    public Map<String, List<String>> getQueryMap() {
        if (queryMap == null) queryMap = splitQuery();
        return queryMap;
    }

    private Map<String, List<String>> splitQuery() {
        var map = new HashMap<String, List<String>>();
        if (exchange.getRequestURI().getQuery() == null) return map;
        for (var element : exchange.getRequestURI().getQuery().split("[&;]")) {
            var split = splitQueryParameter(element);
            var name = split[0];

            if (!map.containsKey(name))
                map.put(name, new ArrayList<>());
            map.get(name).add(split.length == 2 ? split[1] : null);
        }
        for (var entry : map.entrySet()) {
            entry.setValue(ImmutableList.copyOf(entry.getValue()));
        }
        return ImmutableMap.copyOf(map);
    }

    private static String[] splitQueryParameter(String it) {
        var element = it.split("=", 2);
        for (int i = 0; i < element.length; i++)
            element[0] = URLDecoder.decode(element[0], StandardCharsets.UTF_8);
        return element;
    }

    public int getPathSectionLen() {
        if (pathParts == null) {
            pathParts = exchange.getRequestURI().getPath().split("/");
            for (int i = 0; i < pathParts.length; i++)
                pathParts[0] = URLDecoder.decode(pathParts[0], StandardCharsets.UTF_8);
        }
        return pathParts.length;
    }

    public WebServer getServer() {
        return server;
    }

    public String getPathPart(int index) {
        getPathSectionLen();
        return pathParts[index];
    }

    public abstract String path();
}
