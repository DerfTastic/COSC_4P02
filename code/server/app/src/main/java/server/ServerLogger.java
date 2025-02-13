package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ServerLogger {
    private static final FileHandler fh;
    private static final MemHandler mh;

    static {
        try {
            if(new File(Config.CONFIG.log_path+"/log").exists()){
                var file = new File(Config.CONFIG.log_path+"/log");
                var log_attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                var name = dateFormat.format(new Date(log_attributes.creationTime().toMillis()));

                var out_file = new File(Config.CONFIG.log_path+"/" + name + ".zip");
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(out_file));
                ZipEntry e = new ZipEntry(name);
                out.putNextEntry(e);

                byte[] data = Files.readAllBytes(file.toPath());
                out.write(data, 0, data.length);
                out.closeEntry();
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to zip older log file", e);
        }
        try{
            fh = new FileHandler(Config.CONFIG.log_path+"/log");
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        mh = new MemHandler();
    }

    public static void setLogLevel(Level level){
        Logger.getGlobal().setLevel(level);
        for (Handler handler : Logger.getGlobal().getParent().getHandlers()) {
            handler.setLevel(level);
        }
    }

    public static Level getLogLevel(){
        return Logger.getGlobal().getLevel();
    }

    public static void initialize(){
        Logger.getGlobal().addHandler(fh);
        Logger.getGlobal().addHandler(mh);

        setLogLevel(Level.INFO);

        Logger.getGlobal().log(Level.INFO, "Working Directory: " + Paths.get("").toAbsolutePath());
    }

    public static Stream<LogRecord> streamify(){
        return mh.streamify();
    }

    public static void close(){
        for(var handler : Logger.getGlobal().getHandlers()){
            try{
                handler.flush();
            } catch (Exception ignore) {}
            try{
                handler.close();
            } catch (Exception ignore) {}
        }
    }

    public static class MemHandler extends Handler{

        private final LogRecord[] records = new LogRecord[1024];
        private int index = 0;

        public synchronized Stream<LogRecord> streamify(){
            Supplier<Stream<LogRecord>> provider = () -> Arrays.stream(records);
            return Stream.generate(provider)
                    .limit(2)
                    .flatMap(s -> s)
                    .filter(Objects::nonNull)
                    .skip(index%records.length)
                    .limit(Math.max(index, records.length));
        }

        @Override
        public synchronized void publish(LogRecord record) {
            records[index++%records.length]=record;
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
    }
}
