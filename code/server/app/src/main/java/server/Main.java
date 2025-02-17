package server;

import server.infrastructure.WebServerImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for the Ticket Express SaaS.
 * If Config.CONFIG.create_paths is true, a string array containing db_path, dynamic_media_path, and log_path
 * specified in the configuration settings is created. This ensures the creation of these directories for the webserver.
 * Next, this class will instantiate the server logger and webserver for the software service.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        // Create directories from configuration settings
        if(Config.CONFIG.create_paths){
            var paths = new String[]{Config.CONFIG.db_path, Config.CONFIG.dynamic_media_path, Config.CONFIG.log_path};
            for(var path : paths) {
                var p = Path.of(path);
                if(p.getFileName().toString().contains("."))
                    Files.createDirectories(p.getParent());
                else
                    Files.createDirectories(p);
            }
        }
        ServerLogger.initialize(Level.CONFIG);
        try {
            new WebServerImpl().start();
        } catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to start server", e);
        }
    }
}