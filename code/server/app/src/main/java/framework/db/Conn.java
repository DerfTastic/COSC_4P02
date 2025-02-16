package framework.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class Conn implements AutoCloseable {
    protected Connection conn;
    protected final DbManager db;

    public Conn(Connection conn, DbManager db) {
        this.conn = conn;
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
    }

    public boolean isClosed() throws SQLException {
        return conn == null || conn.isClosed();
    }

    protected Connection getConn() {
        return conn;
    }
}