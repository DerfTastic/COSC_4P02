package server.framework.db;

import java.sql.Connection;
import java.sql.SQLException;

public class RwTransaction extends RwConn implements AutoCloseable{
    public RwTransaction(Connection conn, DbManager db) throws SQLException {
        super(conn, db);
        conn.setAutoCommit(false);
    }

    public synchronized void commit() throws SQLException {
        if(!isClosed()){
            getConn().commit();
            getConn().setAutoCommit(true);
            super.close();
        }else throw new RuntimeException("Transaction has already closed");
    }

    public synchronized void rollback() throws SQLException{
        if(!isClosed()){
            getConn().rollback();
            getConn().setAutoCommit(true);
            super.close();
        }else throw new RuntimeException("Transaction has already closed");
    }

    @Override
    public synchronized void close() throws SQLException {
        if(!isClosed())rollback();
        super.close();
    }

    public synchronized void tryCommit() throws SQLException {
        if(!isClosed()){
            getConn().commit();
            getConn().setAutoCommit(true);
            super.close();
        }
    }
}
