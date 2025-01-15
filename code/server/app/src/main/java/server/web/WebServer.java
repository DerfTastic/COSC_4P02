package server.web;

import com.sun.net.httpserver.HttpServer;
import server.db.DbManager;
import server.web.route.RoutesBuilder;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer {
    private static final int PORT = 8080;
    private final HttpServer server;
    private final DbManager database;

    public WebServer() throws IOException, SQLException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        var address = new InetSocketAddress(PORT);
        server = HttpServer.create(address, 0);
        try{
            database = new DbManager();
        }catch (Exception e){
            this.close();
            throw e;
        }

        server.createContext("/", new StaticContentHandler());
        new APIRouteBuilder(database).attachRoutes(server, "/api");

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        Logger.getGlobal().log(Level.INFO, "Server started on http://" + address.getAddress().getHostAddress() + ":" + address.getPort());
    }

    public void close(){
        Logger.getGlobal().log(Level.INFO, "Shutting down");
        try {server.stop(0);} catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to stop web server", e);
        }
        try {database.close();} catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to stop database", e);
        }
        Logger.getGlobal().log(Level.INFO, "Shutdown complete");
    }
}
