package server.framework.web.route;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import server.framework.web.WebServer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Request {
    public final HttpExchange exchange;
    private Map<String, List<String>> queryMap;
    private String[] pathParts;

    public Request(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public abstract void begin() throws ClientError.MethodNotAllowed;

    public int code(){
        return 200;
    }
    public abstract void sendResponse(Request request, int code, byte[] content) throws IOException;

    public void sendResponse(Request request, byte[] content) throws IOException{
        sendResponse(request, code(), content);
    }

    public void sendResponse(Request request, String content) throws IOException{
        sendResponse(request, code(), content);
    }

    public <T> void sendResponse(Request request, T content) throws IOException{
        sendResponse(request, code(), content);
    }

    public void sendResponse(Request request, int code, String content) throws IOException{
        sendResponse(request, code, content.getBytes());
    }

    public <T> void sendResponse(Request request, int code, T message) throws IOException{
        sendResponse(request, code, new Gson().toJson(message));
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
        return (WebServer) exchange.getHttpContext().getAttributes().get(WebServer.class.getName());
    }

    public String getPathPart(int index) {
        getPathSectionLen();
        return pathParts[index];
    }

    public abstract String path();
}
