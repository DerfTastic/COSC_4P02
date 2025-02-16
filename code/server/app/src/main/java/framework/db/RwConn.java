package framework.db;

import java.sql.Connection;
import java.sql.SQLException;


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