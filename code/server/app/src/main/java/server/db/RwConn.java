package server.db;

import java.sql.Connection;
import java.sql.SQLException;


public class RwConn implements AutoCloseable {
    private Connection conn;
    private final DbManager db;

    public RwConn(Connection conn, DbManager db) {
        this.conn = conn;
        this.db = db;
    }

    public NamedPreparedStatement namedPreparedStatement(String sql) throws SQLException{
        return new NamedPreparedStatement(getConn(), sql);
    }

    @Override
    public synchronized void close() throws SQLException {
        if(conn!=null&&!getConn().isClosed())
            db.rePoolRw(getConn());
        else if(conn!=null)
            db.removeRw(getConn());
        conn = null;
    }

    public boolean isClosed() throws SQLException {
        return conn==null||conn.isClosed();
    }

    public Connection getConn() {
        return conn;
    }
}
