package server.framework.web.route;

public interface RouteParameter<T> {
    T construct(Request request) throws Exception;

    @SuppressWarnings("unused")
    default void destruct(Request request, T type) throws Exception {
    }

    default void destructError(Request request, T type) throws Exception {
        this.destruct(request, type);
    }
}
