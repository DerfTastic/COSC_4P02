package server;

import server.infrastructure.WebServerImpl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        Config.init(); // initialize fundamental directories
        ServerLogger.initialize(Level.CONFIG);
        Logger.getGlobal().log(Level.INFO, "Max Heap Size: " + Runtime.getRuntime().maxMemory());
        try{
            new WebServerImpl().start();
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to start server", e);
        }
    }
}