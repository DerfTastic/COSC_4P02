package server.db;

import java.sql.Connection;
import java.sql.SQLException;


public class RoConn extends Conn{
    public RoConn(Connection conn, DbManager db) {
        super(conn, db);
    }

    @Override
    public synchronized void close() throws SQLException {
        db.rePool(this);
        super.close();
    }
}
