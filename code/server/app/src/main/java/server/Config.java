package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Properties;

public class Config implements Serializable {
    public static final Config CONFIG;

    public final Integer web_threads = initialize(256);
    public final Boolean wipe_db_on_start = initialize(false);
    public final Boolean initialize_db_with_data = initialize(false);

    public final String hostname = initialize("localhost");
    public final Integer port = initialize(80);

    public final String db_path = initialize("db/database.db");
    public final String media_path = initialize("media/");
    public final String secrets_path = initialize("secrets");

    static{
        try {
            var properties = new Properties();
            try{
                properties.load(new FileInputStream("server.properties"));
            }catch (IOException ignore){}

            var config = new Config();
            for(var field : Config.class.getFields()){
                if(Modifier.isStatic(field.getModifiers()))continue;
                field.setAccessible(true);

                if(properties.containsKey(field.getName())){
                    String value = properties.getProperty(field.getName()).trim();
                    if(field.getType()==Integer.class){
                        field.set(config, Integer.parseInt(value));
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

    private <T> T initialize(T val){
        return val;
    }
}
