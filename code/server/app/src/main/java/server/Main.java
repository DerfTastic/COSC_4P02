package server;

import server.web.WebServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerLogger.initialize();
        try{
            new WebServer();
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to start server", e);
        }
    }
}