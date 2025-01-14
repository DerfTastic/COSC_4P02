package server.web;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer {
    private static final int PORT = 8080;
    private final HttpServer server;

    public WebServer() throws IOException, SQLException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        var address = new InetSocketAddress(PORT);
        server = HttpServer.create(address, 0);


        server.createContext("/", new StaticContentHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        Logger.getGlobal().log(Level.INFO, "Server started on http://" + address.getAddress().getHostAddress() + ":" + address.getPort());
    }

    public void close(){
        Logger.getGlobal().log(Level.INFO, "Shutting down");
        try {server.stop(0);} catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to stop web server", e);
        }
        Logger.getGlobal().log(Level.INFO, "Shutdown complete");
    }
}
