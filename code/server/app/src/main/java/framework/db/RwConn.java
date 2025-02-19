package framework.db;

import java.sql.SQLException;


public class RwConn extends Conn{
    public RwConn(DbManager db, String id) {
        super(db, id, true);
    }

    @Override
    protected void initialize() throws SQLException {
        conn = db.rw_conn_p(this);
    }

    @Override
    public synchronized void close() throws SQLException {
        db.rePool(this);
        super.close();
    }
}