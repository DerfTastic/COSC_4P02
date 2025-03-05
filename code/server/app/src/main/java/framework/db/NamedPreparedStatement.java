package framework.db;

import framework.util.SqlSerde;
import framework.util.Tuple;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class NamedPreparedStatement implements AutoCloseable {
    private final HashMap<String, Integer> fieldMap;
    private final ArrayList<ResultSet> results = new ArrayList<>();
    protected final PreparedStatement stmt;
    protected final String originalSql;
    protected final String sql;

    protected Conn conn;
    private DbStatistics stats;


    protected NamedPreparedStatement(PreparedStatement stmt, String originalSql, String sql, HashMap<String, Integer> fieldMap) {
        this.stmt = stmt;
        this.originalSql = originalSql;
        this.sql = sql;
        this.fieldMap = fieldMap;
    }

    protected void setConn(Conn conn){
        this.conn = conn;
        this.stats = conn.db.getTracker();
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

    public int getIndex(final String name) throws SQLException {
        Integer index = fieldMap.get(name);
        if(index == null) throw new SQLException("Invalid Named Parameter '"+name+"'. Does not exist");
        return index;
    }

    public <T> void inputObjectParameters(T obj) throws SQLException{
        for(var f: (Iterable<Tuple<String, Object>>)SqlSerde.sqlParameterize(obj)::iterator){
            stmt.setObject(getIndex(f.t1()), f.t2());
        }
    }

    @Override
    public void close() throws SQLException {
        for(var res : results){
            res.close();
        }
        results.clear();
        conn.db.namedPreparedStatementClose(this);
    }

    public ResultSet executeQuery() throws SQLException {
        if(stats!=null)stats.prepared_statement_executed(conn.id, conn.rw);
        var rs = stmt.executeQuery();
        results.add(rs);
        return rs;
    }

    public boolean execute() throws SQLException {
        if(stats!=null)stats.prepared_statement_executed(conn.id, conn.rw);
        return stmt.execute();
    }

    public int executeUpdate() throws SQLException{
        if(stats!=null)stats.prepared_statement_executed(conn.id, conn.rw);
        return stmt.executeUpdate();
    }

    public ResultSet getResultSet() throws SQLException{
        var rs = stmt.getResultSet();
        results.add(rs);
        return rs;
    }
}
