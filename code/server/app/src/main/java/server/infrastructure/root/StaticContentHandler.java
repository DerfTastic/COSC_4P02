package server.infrastructure.root;

import server.Config;
import server.framework.web.request.RequestHandler;
import server.framework.web.annotations.Handler;
import server.framework.web.request.Request;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@Handler
public class StaticContentHandler implements RequestHandler {

    public final boolean checkCachedSources = true;

    private final static class CachedItem{
        Path resolved;
        long last_modified;
        byte[] content;
    }

    private final HashMap<String, CachedItem> cache = new HashMap<>();

    @Override
    public void handle(Request request) throws IOException {
        String requestedPath = request.exchange.getRequestURI().getPath();
        if(!request.exchange.getRequestMethod().equalsIgnoreCase("GET")){
            request.sendResponse(405, "Invalid Method Specified");
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
                request.sendResponse(400, "");
            }
            if(Files.isDirectory(path)){
                request.sendResponse(400, "Not a File");
            }
            if (!Files.exists(path)) {
                request.sendResponse(404, "Not Found");
                return;
            }else{
                cached = new CachedItem();
                cached.resolved = path;
                cached.last_modified = Files.getLastModifiedTime(cached.resolved).toMillis();
                cached.content = Files.readAllBytes(cached.resolved);
                cache.put(requestedPath, cached);
            }
        }

        request.exchange.getResponseHeaders().add("Content-Type", getContentType(cached.resolved.getFileName().toString()));
        if(Config.CONFIG.cache_static_content)
            request.exchange.getResponseHeaders().add("Cache-Control", "max-age=604800");
        request.sendResponse(200, cached.content);
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
