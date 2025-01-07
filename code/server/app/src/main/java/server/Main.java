package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 8080;
    private static final String STATIC_DIR = "../../client"; // Path to your static files

    public static void main(String[] args) throws IOException {
        // Create the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Define the handler for serving static files
        server.createContext("/", exchange -> {
            String requestedPath = exchange.getRequestURI().getPath();
            File file = new File(STATIC_DIR + requestedPath);
            System.out.println(requestedPath);

            if (!file.exists()
                    || file.isDirectory()
                    || requestedPath.contains("..")) {
                sendResponse(exchange, 404, "Not Found");
                return;
            }

            try (InputStream is = new FileInputStream(file)) {
                byte[] content = is.readAllBytes();
                exchange.getResponseHeaders().add("Content-Type", getContentType(file.getName()));
                sendResponse(exchange, 200, content);
            } catch (IOException e) {
                sendResponse(exchange, 500, "Internal Server Error");
                e.printStackTrace();
            }
        });

        // Start the server
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes();
        sendResponse(exchange, statusCode, response);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, byte[] content) throws IOException {
        exchange.sendResponseHeaders(statusCode, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}