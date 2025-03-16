package infrastructure;

import server.infrastructure.DynamicMediaHandler;

import java.util.HashSet;

public class DynamicMediaHandlerSkeleton implements DynamicMediaHandler {
    private final HashSet<Long> present = new HashSet<>();
    private long id = 0;

    @Override
    public byte[] get(long id) {
        throw new RuntimeException("No!");
    }

    @Override
    public void delete(long id) {
        present.remove(id);
    }

    @Override
    public long add(byte[] data) {
        present.add(++id);
        return id;
    }

    public boolean present(long id) {
        return present.contains(id);
    }
}
