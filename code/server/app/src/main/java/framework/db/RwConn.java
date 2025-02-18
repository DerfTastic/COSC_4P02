package framework.db;

import java.sql.SQLException;


public class RwConn extends Conn{
    public RwConn(DbManager db) {
        super(db);
    }

    @Override
    protected void initialize() throws SQLException {
        conn = db.rw_conn_p();
    }

    @Override
    public synchronized void close() throws SQLException {
        db.rePool(this);
        super.close();
    }
}