package framework.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class Conn implements AutoCloseable {
    private boolean first = true;
    protected Connection conn;
    protected final DbManager db;
    protected final boolean rw;
    protected final String id;
    protected long acquired;

    protected Conn(DbManager db, String id, boolean rw) {
        this.db = db;
        this.rw = rw;
        this.id = id;
    }

    public NamedPreparedStatement namedPreparedStatement(String sql) throws SQLException {
        var stmt = new NamedPreparedStatement(this, sql);
        return stmt;
    }

    public Statement createStatement() throws SQLException{
        var stmt = new StatementM(this);
        return stmt;
    }

    @Override
    public synchronized void close() throws SQLException {
        conn = null;
        first = false;
    }

    public boolean isClosed() throws SQLException {
        return conn == null || conn.isClosed();
    }

    protected abstract void initialize() throws SQLException;
    protected synchronized Connection getConn() throws SQLException {
        if(first){
            initialize();
            first = false;
        }
        return conn;
    }

    public void commit() throws SQLException {
        if(!isClosed())
            conn.commit();
    }

    public void rollback() throws SQLException{
        if(!isClosed())
            conn.rollback();
    }
}