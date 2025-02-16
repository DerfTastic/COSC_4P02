package framework.util.func;

@FunctionalInterface
public interface Consume2<T1, T2> {
    void call(T1 t1, T2 t2) throws Exception;
}
