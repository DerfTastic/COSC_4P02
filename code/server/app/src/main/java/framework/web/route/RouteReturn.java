package framework.web.route;

import framework.web.request.Request;

public interface RouteReturn<T> {
    void send(Request request, T data) throws Exception;
}
