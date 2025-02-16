package framework.util.func;

@FunctionalInterface
public interface Function0<R> {
    R call() throws Exception;
}
