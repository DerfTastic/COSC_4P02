package server.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public class RwConn extends Conn{
    public RwConn(Connection conn, DbManager db) {
        super(conn, db);
    }

    @Override
    public synchronized void close() throws SQLException {
        db.rePool(this);
        super.close();
    }
}