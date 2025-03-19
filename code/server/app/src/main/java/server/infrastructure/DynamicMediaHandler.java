package server.infrastructure;

public interface DynamicMediaHandler {
    byte[] get(long id);
    void delete(long id);
    long add(byte[] data);
}
