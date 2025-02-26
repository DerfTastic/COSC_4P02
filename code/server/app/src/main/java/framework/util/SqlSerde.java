package framework.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    public static <T> T sqlSingle(ResultSet rs, Class<T> clazz) throws SQLException{
        var res = sqlList(rs, clazz);
        if(res.isEmpty())
            throw new SQLException("Expected a single result got none");
        if(res.size() > 1)
            throw new SQLException("Expected single result got more");
        return res.get(0);
    }

    public static <T> ArrayList<T> sqlList(ResultSet rs, Class<T> clazz) throws SQLException {
        var list = new ArrayList<T>();
        try{
            var constructor = clazz.getDeclaredConstructor();
            var fields = clazz.getFields();
            while(rs.next()){
                var instance = constructor.newInstance();
                for(var field : fields){
                    var name = field.getName();
                    if(field.getType().equals(int.class)){
                        field.setInt(instance, rs.getInt(name));
                    }else if(field.getType().equals(float.class)){
                        field.setFloat(instance, rs.getFloat(name));
                    }else if(field.getType().equals(boolean.class)){
                        field.setBoolean(instance, rs.getBoolean(name));
                    }else if(field.getType().equals(short.class)){
                        field.setShort(instance, rs.getShort(name));
                    }else if(field.getType().equals(long.class)){
                        field.setLong(instance, rs.getLong(name));
                    }else if(field.getType().equals(byte.class)){
                        field.setByte(instance, rs.getByte(name));
                    }else if(field.getType().equals(double.class)){
                        field.setDouble(instance, rs.getDouble(name));
                    }else if(field.getType().equals(String.class)) {
                        field.set(instance, rs.getString(name));
                    }else if(field.getType().equals(Long.class)){
                        field.set(field.getType(), rs.getLong(name));
                    }else if(field.getType().equals(JsonObject.class)){
                        var t = rs.getString(name);
                        field.set(instance, JsonParser.parseString(t==null?"{}":t).getAsJsonObject());
                    }else if(field.getType().equals(JsonArray.class)){
                        var t = rs.getString(name);
                        field.set(instance, JsonParser.parseString(t==null?"[]":t).getAsJsonArray());
                    }else throw new RuntimeException("Invalid field type: " + field);
                }
                list.add(instance);
            }
            return list;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
