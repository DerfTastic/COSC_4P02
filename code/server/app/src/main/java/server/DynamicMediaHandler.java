package server;

import java.util.HashMap;

public class DynamicMediaHandler {

    private long id = 0;
    private final HashMap<Long, byte[]> content = new HashMap<>();
    private final String path = Config.CONFIG.media_path;
    private final long max_cache_size = 1L << 30;

    public synchronized byte[] get(long id) {
        return content.getOrDefault(id, new byte[0]);
    }

    public synchronized void delete(long id){
        content.remove(id);
    }

    public synchronized long add(byte[] data){
        content.put(++id, data);
        return id;
    }
}
