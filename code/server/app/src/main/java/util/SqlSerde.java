package util;

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
        if(res.size() != 1) throw new SQLException("Expected single result");
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
                    if(field.getType().equals(int.class)){
                        field.setInt(instance, rs.getInt(field.getName()));
                    }else if(field.getType().equals(float.class)){
                        field.setFloat(instance, rs.getFloat(field.getName()));
                    }else if(field.getType().equals(boolean.class)){
                        field.setBoolean(instance, rs.getBoolean(field.getName()));
                    }else if(field.getType().equals(short.class)){
                        field.setShort(instance, rs.getShort(field.getName()));
                    }else if(field.getType().equals(long.class)){
                        field.setLong(instance, rs.getLong(field.getName()));
                    }else if(field.getType().equals(byte.class)){
                        field.setByte(instance, rs.getByte(field.getName()));
                    }else if(field.getType().equals(double.class)){
                        field.setDouble(instance, rs.getDouble(field.getName()));
                    }else if(field.getType().equals(String.class)){
                        field.set(instance, rs.getString(field.getName()));
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
