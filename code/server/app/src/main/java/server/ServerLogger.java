package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
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
            if(!new File("./logs").exists())
                Files.createDirectory(Path.of("./logs"));
            if(new File("./logs/log").exists()){
                var file = new File("./logs/log");
                var name = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

                var on = new Date(name.creationTime().toMillis()).toString();
                var out_file = new File("./logs/" + on + ".zip");
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(out_file));
                ZipEntry e = new ZipEntry(on);
                out.putNextEntry(e);

                byte[] data = Files.readAllBytes(file.toPath());
                out.write(data, 0, data.length);
                out.closeEntry();

                out.close();
            }
            fh = new FileHandler("./logs/log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mh = new MemHandler();
    }

    public static void initialize(){
        Logger.getGlobal().addHandler(fh);
        Logger.getGlobal().addHandler(mh);

        Logger.getGlobal().setLevel(Level.ALL);
        for (Handler handler : Logger.getGlobal().getParent().getHandlers()) {
            handler.setLevel(Level.ALL);
        }

        Logger.getGlobal().log(Level.FINE, "Working Directory: " + Paths.get("").toAbsolutePath());
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
