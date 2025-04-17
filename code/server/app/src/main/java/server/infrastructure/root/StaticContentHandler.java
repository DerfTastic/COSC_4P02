package server.infrastructure.root;

import framework.web.error.ClientError;
import framework.web.request.RequestHandler;
import framework.web.annotations.Handler;
import framework.web.request.Request;
import server.infrastructure.param.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@Handler
@SuppressWarnings("unused")
public class StaticContentHandler implements RequestHandler {

    public final boolean checkCachedSources = true;

    public final static class CachedItem{
        Path resolved;
        long last_modified;
        byte[] content;
    }

    private final HashMap<String, String> urlMap = new HashMap<>();
    private final HashMap<String, CachedItem> cache = new HashMap<>();
    private final String rootPath;
    private final boolean doCache;

    public StaticContentHandler(@Config String static_content_path, @Config boolean cache_static_content){
        this.rootPath = static_content_path;
        this.doCache = cache_static_content;
        urlMap.put("/account", "/partials/profile");
        urlMap.put("/user/profile", "/partials/profile");
    }

    @Override
    public void handle(Request request) throws IOException {
        String reqPath = request.exchange.getRequestURI().getPath();
        if(!request.exchange.getRequestMethod().equalsIgnoreCase("GET")){
            request.sendResponse(405, "Invalid Method Specified");
            return;
        }
        if(urlMap.containsKey(reqPath))
            reqPath = urlMap.get(reqPath);

        CachedItem cached;
        try{
            cached = get(reqPath);
        }catch (ClientError e){
            request.sendResponse(e.code, e.getMessage());
            return;
        }

        request.exchange.getResponseHeaders().add("Content-Type", getContentType(cached.resolved.getFileName().toString()));
        if(doCache)
            request.exchange.getResponseHeaders().add("Cache-Control", "max-age=604800");
        request.sendResponse(200, cached.content);
    }

    public CachedItem get(String reqPath) throws ClientError, IOException {
        var cached = cache.get(reqPath);
        cached:
        if(cached != null){
            if(checkCachedSources){
                if(!Files.exists(cached.resolved)){
                    cache.remove(reqPath);
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
            StringBuilder builder = new StringBuilder(reqPath);
            if(builder.toString().endsWith("/"))
                builder.append("index");
            else if(Files.isDirectory(Path.of(rootPath +builder)))
                builder.append("/index");
            if(!builder.toString().contains("."))
                if(Files.exists(Path.of(rootPath + builder + ".html")))
                    builder.append(".html");
                else if(Files.exists(Path.of(rootPath + builder +".hbs")))
                    builder.append(".hbs");

            var path = Path.of(rootPath + builder);

            if(reqPath.contains(".."))
                throw new ClientError(400, "Not a File");
            if(Files.isDirectory(path))
                throw new ClientError(400, "Not a File");
            if (!Files.exists(path))
                throw new ClientError(404, "Not Found");

            cached = new CachedItem();
            cached.resolved = path;
            cached.last_modified = Files.getLastModifiedTime(cached.resolved).toMillis();
            cached.content = Files.readAllBytes(cached.resolved);
            cache.put(reqPath, cached);
        }
        return cached;
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
