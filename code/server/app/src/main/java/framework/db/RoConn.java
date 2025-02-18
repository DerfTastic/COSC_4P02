package framework.db;

import java.sql.Connection;
import java.sql.SQLException;


public class RoConn extends Conn{
    public RoConn(DbManager db) {
        super(db);
    }

    @Override
    protected void initialize() throws SQLException {
        conn = db.ro_conn_p();
    }

    @Override
    public synchronized void close() throws SQLException {
        db.rePool(this);
        super.close();
    }
}
