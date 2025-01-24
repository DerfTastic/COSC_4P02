package server.web.route;

public interface RouteReturn<T> {
    void send(Request request, T data) throws Exception;
}
