package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TypeReflect {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS;
    static {
        try {
            Map<Class<?>, Class<?>> types = new HashMap<Class<?>, Class<?>>();
            types.put(boolean.class, Boolean.class);
            types.put(byte.class, Byte.class);
            types.put(char.class, Character.class);
            types.put(double.class, Double.class);
            types.put(float.class, Float.class);
            types.put(int.class, Integer.class);
            types.put(long.class, Long.class);
            types.put(short.class, Short.class);
            types.put(void.class, Void.class);
            PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(types);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> wrapPrimitives(Class<?> clazz) {
        return clazz.isPrimitive() ? PRIMITIVE_WRAPPERS.get(clazz) : clazz;
    }
}
