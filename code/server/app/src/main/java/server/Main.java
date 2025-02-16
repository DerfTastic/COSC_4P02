package server;

import server.framework.web.WebServer;
import server.infrastructure.WebServerImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) throws IOException {
        if(Config.CONFIG.create_paths){
            var paths = new String[]{Config.CONFIG.db_path, Config.CONFIG.dynamic_media_path, Config.CONFIG.log_path};
            for(var path : paths){
                var p = Path.of(path);
                if(p.getFileName().toString().contains("."))
                    Files.createDirectories(p.getParent());
                else
                    Files.createDirectories(p);
            }
        }
        ServerLogger.initialize();
        try{
            new WebServerImpl().start();
        }catch (Exception e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to start server", e);
        }
    }
}