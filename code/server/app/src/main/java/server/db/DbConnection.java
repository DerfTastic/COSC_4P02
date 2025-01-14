package server.db;

import java.sql.Connection;
import java.sql.SQLException;


public class DbConnection implements AutoCloseable {
    public final Connection conn;
    private final DbManager db;

    public DbConnection(Connection conn, DbManager db) {
        this.conn = conn;
        this.db = db;
    }

    public Transaction transaction() throws SQLException {
        return new Transaction(this);
    }

    @Override
    public void close() throws SQLException {
        if(!conn.isClosed()) db.reAddConnection(this);
    }
}
