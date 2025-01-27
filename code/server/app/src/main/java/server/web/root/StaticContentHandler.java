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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Handler
public class StaticContentHandler implements HttpHandler {

    private final HashMap<String, Tuple<Long, byte[]>> cache = new HashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestedPath = exchange.getRequestURI().getPath();
        if(!exchange.getRequestMethod().equalsIgnoreCase("GET")){
            Util.sendResponse(exchange, 405, "Invalid Method Specified");
            return;
        }

        if (requestedPath.endsWith("/")) {
            requestedPath += "index.html";
        }
        if(!requestedPath.contains(".")){
            if(new File(Config.CONFIG.static_content_path +requestedPath+".html").exists())
                requestedPath += ".html";
            else if(new File(Config.CONFIG.static_content_path + requestedPath+".hbs").exists())
                requestedPath += ".hbs";
        }
        File file = new File(Config.CONFIG.static_content_path + requestedPath);

        if (!file.exists()
                || file.isDirectory()
                || requestedPath.contains("..")) {
            Util.sendResponse(exchange, 404, "Not Found");
            return;
        }

        byte[] content;
        var entry = cache.getOrDefault(requestedPath, null);
        if (entry == null || entry.t1 < file.lastModified()) {
            try (InputStream is = new FileInputStream(file)) {
                content = is.readAllBytes();
                cache.put(requestedPath, new Tuple<>(file.lastModified(), content));
            } catch (IOException e) {
                Util.sendResponse(exchange, 500, "Internal Server Error");
                Logger.getGlobal().log(Level.SEVERE, "", e);
                return;
            }
        } else {
            content = entry.t2;
        }
        exchange.getResponseHeaders().add("Content-Type", getContentType(file.getName()));
        if(Config.CONFIG.cache_static_content)
            exchange.getResponseHeaders().add("Cache-Control", "max-age=604800");
        Util.sendResponse(exchange, 200, content);
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
