package server.web.route;

public interface RouteReturn<T> {
    void send(RouteImpl.Request request, T data) throws Exception;
}
