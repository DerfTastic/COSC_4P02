package framework.db;

import java.sql.SQLException;


public class RoConn extends Conn{
    public RoConn(DbManager db, String id) {
        super(db, id, false);
    }

    @Override
    protected void initialize() throws SQLException {
        conn = db.ro_conn_p(this);
    }

    @Override
    public synchronized void close() throws SQLException {
        db.rePool(this);
        super.close();
    }
}
