package server.framework.web.route;

import server.framework.web.request.Request;

public interface RouteReturn<T> {
    void send(Request request, T data) throws Exception;
}
