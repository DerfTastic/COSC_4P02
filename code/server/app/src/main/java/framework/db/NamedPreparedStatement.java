package framework.db;

import framework.web.ServerStatistics;
import framework.util.SqlSerde;
import framework.util.Tuple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class NamedPreparedStatement implements AutoCloseable {
    private final HashMap<String, Integer> fieldMap;
    private final static HashMap<String, Tuple<String, HashMap<String, Integer>>> fieldMapCache = new HashMap<>();
    private final static HashMap<Tuple<Connection, String>, PreparedStatement> preparedMap = new HashMap<>();
    private final PreparedStatement stmt;

    ServerStatistics stats;

    private static Tuple<String, HashMap<String, Integer>> initialize(String sqlO){
        synchronized (fieldMapCache){
            if(fieldMapCache.containsKey(sqlO)){
                return fieldMapCache.get(sqlO);
            }
        }
        String sql = sqlO;
        int pos;
        int index = 1;
        final HashMap<String, Integer> fieldMap = new HashMap<>();
        while((pos = sql.indexOf(":")) != -1) {
            var start = pos;
            pos++;
            while(pos < sql.length() && validChar(sql.charAt(pos)))pos++;

            int idx;
            if(fieldMap.containsKey(sql.substring(start,pos))){
                idx = fieldMap.get(sql.substring(start,pos));
            }else{
                fieldMap.put(sql.substring(start,pos), index);
                idx = index;
                index += 1;
            }
            sql = sql.substring(0, start) + "?" + idx + sql.substring(pos);
        }
        var ret = new Tuple<>(sql, fieldMap);
        synchronized (fieldMapCache){
            fieldMapCache.put(sqlO, ret);
        }
        return ret;
    }

    private static boolean validChar(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';
    }

    public NamedPreparedStatement(Connection conn, String sql) throws SQLException {
        this(conn, initialize(sql));
    }

    public void setInt(String name, int value) throws SQLException {
        stmt.setInt(getIndex(name), value);
    }

    public void setLong(String name, long value) throws SQLException {
        stmt.setLong(getIndex(name), value);
    }

    public void setString(String name, String value) throws SQLException {
        stmt.setString(getIndex(name), value);
    }

    public void setBoolean(String name, boolean value) throws SQLException {
        stmt.setBoolean(getIndex(name), value);
    }

    public void setDouble(String name, double value) throws SQLException {
        stmt.setDouble(getIndex(name), value);
    }

    /**
     * This constructor takes a connection and tuple returned by initialize(sql) as arguments to instantiate this class.
     * @param conn the connection to the database
     * @param sql tuple containing a string and a String-Integer hashmap
     * @throws SQLException if an error occurs preparing SQL statement
     */
    private NamedPreparedStatement(Connection conn, Tuple<String, HashMap<String, Integer>> sql) throws SQLException {
        var tuple = new Tuple<>(conn, sql.t1);
        synchronized (preparedMap){
            if(preparedMap.containsKey(tuple)){
                this.stmt = preparedMap.get(tuple);
            }else{
                this.stmt = conn.prepareStatement(sql.t1);
                preparedMap.put(tuple, this.stmt);
            }
        }
        this.stmt.clearParameters();
        fieldMap = sql.t2;
    }

    public int getIndex(final String name) throws SQLException {
        Integer index = fieldMap.get(name);
        if(index == null) throw new SQLException("Invalid Named Parameter '"+name+"'. Does not exist");
        return index;
    }

    public <T> void inputObjectParameters(T obj) throws SQLException{
        for(var f: (Iterable<Tuple<String, Object>>)SqlSerde.sqlParameterize(obj)::iterator){
            stmt.setObject(getIndex(f.t1), f.t2);
        }
    }

    private final ArrayList<ResultSet> results = new ArrayList<>();

    @Override
    public void close() throws SQLException {
        for(var res : results){
            res.close();
        }
        results.clear();
//        stmt.close();
    }

    public ResultSet executeQuery() throws SQLException {
        if(stats!=null)stats.executed_prepared_statement();
        var rs = stmt.executeQuery();
        results.add(rs);
        return rs;
    }

    public boolean execute() throws SQLException {
        if(stats!=null)stats.executed_prepared_statement();
        return stmt.execute();
    }

    public int executeUpdate() throws SQLException{
        if(stats!=null)stats.executed_prepared_statement();
        return stmt.executeUpdate();
    }

    public ResultSet getResultSet() throws SQLException{
        var rs = stmt.getResultSet();
        results.add(rs);
        return rs;
    }
}
