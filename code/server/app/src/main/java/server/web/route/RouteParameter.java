package server.web.route;

public interface RouteParameter<T> {
    T construct(RouteImpl.Request request) throws Exception;

    @SuppressWarnings("unused")
    default void destruct(RouteImpl.Request request, T type) throws Exception {
    }

    default void destructError(RouteImpl.Request request, T type) throws Exception {
        this.destruct(request, type);
    }
}
