package framework.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class Conn implements AutoCloseable {
    private boolean first = true;
    protected Connection conn;
    protected final DbManager db;

    public Conn(DbManager db) {
        this.db = db;
    }

    public NamedPreparedStatement namedPreparedStatement(String sql) throws SQLException {
        var stmt = new NamedPreparedStatement(getConn(), sql);
        stmt.stats = this.db.getStatsTracker();
        return stmt;
    }

    public Statement createStatement() throws SQLException{
        var stmt = new StatementM(getConn().createStatement());
        stmt.stats = this.db.getStatsTracker();
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