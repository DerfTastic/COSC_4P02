package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Secrets {
    private final static Properties properties = new Properties();

    static{
        try {
            properties.load(new FileInputStream(Config.CONFIG.secrets_path));
        } catch (IOException e) {
            Logger.getGlobal().log(Level.WARNING, "Cannot load secrets file. Some features might not work properly or be missing", e);
        }
    }

    public static String get(String key){
        return properties.getProperty(key);
    }

    public static String getOr(String key, String or){
        return (String) properties.getOrDefault(key, or);
    }
}
