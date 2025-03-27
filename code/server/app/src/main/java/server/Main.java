package server;

import server.infrastructure.WebServerImpl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        var config = Config.init(); // initialize fundamental directories
        ServerLogger.initialize(Level.CONFIG, config.log_path);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Logger.getGlobal().log(Level.SEVERE, "Unhandled exception on thread " + t, e);
        });
        Logger.getGlobal().log(Level.INFO, "Max Heap Size: " + Runtime.getRuntime().maxMemory());
        try{
            new WebServerImpl(config).start();
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to start server", e);
        }
    }
}