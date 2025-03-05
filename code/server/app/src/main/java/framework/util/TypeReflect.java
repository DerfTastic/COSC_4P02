package framework.util;

import java.util.Map;

public class TypeReflect {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS;
    static {
        try {
            PRIMITIVE_WRAPPERS = Map.of(
                    boolean.class, Boolean.class,
                    byte.class, Byte.class,
                    char.class, Character.class,
                    double.class, Double.class,
                    float.class, Float.class,
                    int.class, Integer.class,
                    long.class, Long.class,
                    short.class, Short.class,
                    void.class, Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> wrapPrimitives(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_WRAPPERS.get(clazz) : clazz;
    }
}
