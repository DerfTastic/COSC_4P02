package server;

import server.web.WebServer;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        Logger.getGlobal().setLevel(Level.ALL);
        for (Handler handler : Logger.getGlobal().getParent().getHandlers()) {
            handler.setLevel(Level.ALL);
        }

        try{
            new WebServer();
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to start server", e);
        }
    }
}