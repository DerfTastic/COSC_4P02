package server;

import java.util.HashMap;

public class DynamicMediaHandler {

    private int id = 0;
    private final HashMap<Integer, byte[]> content = new HashMap<>();

    public synchronized byte[] get(int id) {
        return content.getOrDefault(id, new byte[0]);
    }

    public synchronized void delete(int id){
        content.remove(id);
    }

    public synchronized int add(byte[] data){
        content.put(++id, data);
        return id;
    }
}
