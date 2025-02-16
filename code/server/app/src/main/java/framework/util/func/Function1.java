package framework.util.func;

@FunctionalInterface
public interface Function1<R, T1> {
    R call(T1 t1) throws Exception;
}
