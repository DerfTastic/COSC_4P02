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
    public static final Config CONFIG; // Variable to another instance of this class?

    public final Integer web_threads = initialize(256);
    public final Boolean wipe_db_on_start = initialize(false);
    public final Boolean store_db_in_memory = initialize(false);

    public final String hostname = initialize("localhost");
    public final Integer port = initialize(80);

    public final String db_path = initialize("db/database.db");
    public final String dynamic_media_path = initialize("media");
    public final Long dynamic_media_cache_size = initialize(1L<<30);
    public final String secrets_path = initialize("secrets");

    public final String log_path = initialize("logs");

    public final String static_content_path = initialize("site");

    public final Boolean cache_static_content = initialize(false);

    public final Boolean create_paths = initialize(true);

    static{
        outer:
        try {
            var properties = new Properties();
            if (!Files.exists(Path.of("server.properties"))){
                CONFIG = new Config();

                for(var field : Config.class.getFields()) {
                    properties.put(field.getName(), field.get(CONFIG).toString());
                }
                properties.store(new FileOutputStream("server.properties"), null);
                break outer;
            }
            try{
                properties.load(new FileInputStream("server.properties"));
            }catch (IOException e){
                Logger.getGlobal().log(Level.WARNING, "Error while loading properties", e);
            }

            var config = new Config();
            for(var field : Config.class.getFields()){
                if(Modifier.isStatic(field.getModifiers()))continue;
                field.setAccessible(true);

                if(properties.containsKey(field.getName())){
                    String value = properties.getProperty(field.getName()).trim();
                    if(field.getType()==Integer.class){
                        field.set(config, Integer.parseInt(value));
                    }else if(field.getType()== Long.class){
                        field.set(config, Long.parseLong(value));
                    }else if(field.getType()==Boolean.class){
                        field.set(config, Boolean.parseBoolean(value));
                    }else{
                        field.set(config, value);
                    }
                }else if(field.get(config) == null){
                    throw new RuntimeException("Property not specified " + field);
                }
            }
            CONFIG = config;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() throws IOException {
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
    }

    private <T> T initialize(T val){
        return val;
    }
}
