package server.web.root;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.Config;
import server.web.Util;
import server.web.annotations.Handler;
import util.Tuple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Handler
public class StaticContentHandler implements HttpHandler {

    public final boolean checkCachedSources = true;

    private final static class CachedItem{
        Path resolved;
        long last_modified;
        byte[] content;
    }

    private final HashMap<String, CachedItem> cache = new HashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestedPath = exchange.getRequestURI().getPath();
        if(!exchange.getRequestMethod().equalsIgnoreCase("GET")){
            Util.sendResponse(exchange, 405, "Invalid Method Specified");
            return;
        }

        var cached = cache.get(requestedPath);
        cached:
        if(cached != null){
            if(checkCachedSources){
                if(!Files.exists(cached.resolved)){
                    cache.remove(requestedPath);
                    break cached;
                }
                var time = Files.getLastModifiedTime(cached.resolved).toMillis();
                if(time>cached.last_modified){
                    cached.content = Files.readAllBytes(cached.resolved);
                    cached.last_modified = time;
                }
            }
        }
        if(cached==null){
            StringBuilder builder = new StringBuilder(requestedPath);
            if(builder.toString().endsWith("/"))
                builder.append("index");
            else if(Files.isDirectory(Path.of(Config.CONFIG.static_content_path+builder)))
                builder.append("/index");
            if(!builder.toString().contains("."))
                if(Files.exists(Path.of(Config.CONFIG.static_content_path + builder + ".html")))
                    builder.append(".html");
                else if(Files.exists(Path.of(Config.CONFIG.static_content_path + builder +".hbs")))
                    builder.append(".hbs");

            var path = Path.of(Config.CONFIG.static_content_path + builder);

            if(requestedPath.contains("..")){
                Util.sendResponse(exchange, 400, "");
            }
            if(Files.isDirectory(path)){
                Util.sendResponse(exchange, 400, "Not a File");
            }
            if (!Files.exists(path)) {
                Util.sendResponse(exchange, 404, "Not Found");
                return;
            }else{
                cached = new CachedItem();
                cached.resolved = path;
                cached.last_modified = Files.getLastModifiedTime(cached.resolved).toMillis();
                cached.content = Files.readAllBytes(cached.resolved);
                cache.put(requestedPath, cached);
            }
        }

        exchange.getResponseHeaders().add("Content-Type", getContentType(cached.resolved.getFileName().toString()));
        if(Config.CONFIG.cache_static_content)
            exchange.getResponseHeaders().add("Cache-Control", "max-age=604800");
        Util.sendResponse(exchange, 200, cached.content);
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".hbs")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
