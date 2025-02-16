package framework.util.func;

@FunctionalInterface
public interface Function2<R, T1, T2> {
    R call(T1 t1, T2 t2) throws Exception;
}
