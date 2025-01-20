package server.db;

import java.sql.Connection;
import java.sql.SQLException;


public class DbConnection implements AutoCloseable {
    public Connection conn;
    private final DbManager db;

    public DbConnection(Connection conn, DbManager db) {
        this.conn = conn;
        this.db = db;
    }

    public NamedPreparedStatement namedPreparedStatement(String sql) throws SQLException{
        return new NamedPreparedStatement(conn, sql);
    }

    public Transaction transaction() throws SQLException {
        return new Transaction(this);
    }

    @Override
    public synchronized void close() throws SQLException {
        if(!conn.isClosed()) db.reAddConnection(conn);
        conn = null;
    }
}
