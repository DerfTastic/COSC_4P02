package framework.util.func;

@FunctionalInterface
public interface Consume1<T1> {
    void call(T1 t1) throws Exception;
}
