package server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
    public final Integer web_threads = initialize(256);
    public final Boolean wipe_db_on_start = initialize(false);
    public final Boolean store_db_in_memory = initialize(false);
    public final String hostname = initialize("localhost");
    public final Integer port = initialize(80);

    public final Integer backlog = initialize(0);

    public final String db_path = initialize("db/database.db");
    public final String dynamic_media_path = initialize("media");
    public final Long dynamic_media_cache_size = initialize(1L<<30);
    public final String secrets_path = initialize("secrets");

    public final String log_path = initialize("logs");

    public final String static_content_path = initialize("site");

    public final Boolean cache_static_content = initialize(false);

    public final Boolean create_paths = initialize(true);

    public final Boolean send_mail = initialize(false);
    public final Boolean send_mail_on_register = initialize(false);
    public final Boolean send_mail_on_login = initialize(false);

    public final String url_root = initialize("http://localhost:80");

    private Properties to_properties() {
        var properties = new Properties();
        for (var field : Config.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue; // Skip if static
            try {
                properties.put(field.getName(), field.get(this).toString());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return properties;
    }

    public Config(){}

    public static Properties config(String... args){
        var properties = new Properties();
        for(int i = 0; i < args.length; i ++){
            properties.put(args[i++], args[i]);
        }
        return properties;
    }

    public Config(String... args){
        this(config(args));

    }
    public Config(Properties properties){
        try{
            for (var field : Config.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue; // Skip if static
                field.setAccessible(true);

                if (properties.containsKey(field.getName())) {
                    String value = properties.getProperty(field.getName()).trim();
                    if (field.getType() == Integer.class) {
                        field.set(this, Integer.parseInt(value));
                    } else if (field.getType() == Long.class) {
                        field.set(this, Long.parseLong(value));
                    } else if (field.getType() == Boolean.class) {
                        field.set(this, Boolean.parseBoolean(value));
                    } else {
                        field.set(this, value);
                    }
                } else if (field.get(this) == null) {
                    throw new RuntimeException("Property not specified " + field);
                }
            }
        }catch (IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    public static Config init() throws IOException {
        if (!Files.exists(Path.of("server.properties"))) {
            var config = new Config();
            config.to_properties().store(new FileOutputStream("server.properties"), null);
            return config;
        }

        Config config;
        var properties = new Properties();
        try {
            properties.load(new FileInputStream("server.properties"));
            config = new Config(properties);
        } catch (IOException e) {
            Logger.getGlobal().log(Level.WARNING, "Error while loading properties", e);
            throw new RuntimeException(e);
        }

        if(config.create_paths){
            var paths = new String[]{config.db_path, config.dynamic_media_path, config.log_path};
            for(var path : paths){
                var p = Path.of(path);
                if(p.getFileName().toString().contains("."))
                    Files.createDirectories(p.getParent());
                else
                    Files.createDirectories(p);
            }
        }
        return config;
    }

    /**
     * This method exists purely so we can define default values in a semi non-annoying way without java treating our
     * initializers as a constant expression and inlining their values preventing us from changing them if loaded from a file.
     */
    private <T> T initialize(T val){
        return val;
    }
}
