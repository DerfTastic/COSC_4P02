package util.func;

@FunctionalInterface
public interface Consume3<T1, T2, T3> {
    void call(T1 t1, T2 t2, T3 t3) throws Exception;
}