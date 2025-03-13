package framework.util;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class SqlSerde {

    private static <T> Stream<Field> memberFields(T obj){
        return Arrays.stream(obj.getClass().getFields())
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .filter(f -> !Modifier.isStatic(f.getModifiers()));
    }

    public static <T> Stream<Tuple<String, Object>> sqlParameterize(T obj){
        return memberFields(obj).map(f -> {
            try {
                return new Tuple<>(":"+f.getName(), f.get(obj));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public interface Map<T>{
        T call(ResultSet rs) throws SQLException;
    }

    public static <T> T sqlSingle(ResultSet rs, Map<T> func) throws SQLException {
        var res = sqlList(rs, func);
        if(res.isEmpty())
            throw new SQLException("Expected a single result got none");
        if(res.size() > 1)
            throw new SQLException("Expected single result got more");
        return res.getFirst();
    }

    public static <T> T sqlSingle(ResultSet rs, Class<T> clazz) throws SQLException{
        var res = sqlList(rs, clazz);
        if(res.isEmpty())
            throw new SQLException("Expected a single result got none");
        if(res.size() > 1)
            throw new SQLException("Expected single result got more");
        return res.getFirst();
    }

    public static <T> ArrayList<T> sqlList(ResultSet rs, Map<T> func) throws SQLException {
        var list = new ArrayList<T>();
        while(rs.next()){
            list.add(func.call(rs));
        }
        return list;
    }

    public static <T> ArrayList<T> sqlList(ResultSet rs, Class<T> clazz) throws SQLException {
        var list = new ArrayList<T>();
        try{
            var constructor = clazz.getDeclaredConstructor();
            var fields = clazz.getFields();
            while(rs.next()){
                var instance = constructor.newInstance();
                for(var field : fields) {
                    var name = field.getName();
                    switch (field.getType()) {
                        case Class<?> cl when cl == byte.class -> field.setByte(instance, rs.getByte(name));
                        case Class<?> cl when cl == short.class -> field.setShort(instance, rs.getShort(name));
                        case Class<?> cl when cl == int.class -> field.setInt(instance, rs.getInt(name));
                        case Class<?> cl when cl == long.class -> field.setLong(instance, rs.getLong(name));
                        case Class<?> cl when cl == float.class -> field.setFloat(instance, rs.getFloat(name));
                        case Class<?> cl when cl == double.class -> field.setDouble(instance, rs.getDouble(name));
                        case Class<?> cl when cl == boolean.class -> field.setBoolean(instance, rs.getBoolean(name));

                        case Class<?> cl when cl == Byte.class -> field.set(instance, rs.getByte(name));
                        case Class<?> cl when cl == Short.class -> field.set(instance, rs.getShort(name));
                        case Class<?> cl when cl == Integer.class -> field.set(instance, rs.getInt(name));
                        case Class<?> cl when cl == Long.class -> field.set(instance, rs.getLong(name));
                        case Class<?> cl when cl == Float.class -> field.set(instance, rs.getFloat(name));
                        case Class<?> cl when cl == Double.class -> field.set(instance, rs.getDouble(name));
                        case Class<?> cl when cl == Boolean.class -> field.set(instance, rs.getBoolean(name));

                        case Class<?> cl when cl == String.class -> field.set(instance, rs.getString(name));

                        case Class<?> cl when cl == JSONObject.class -> {
                            var t = rs.getString(name);
                            var o = JSON.parseObject(t == null ? "{}" : t);
                            field.set(instance, o);
                        }
                        case Class<?> cl when cl == JSONArray.class -> {
                            var t = rs.getString(name);
                            var o = JSON.parseArray(t == null ? "[]" : t);
                            field.set(instance, o);
                        }
                        default -> throw new RuntimeException("Invalid field type: " + field);
                    }
                }
                list.add(instance);
            }
            return list;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
