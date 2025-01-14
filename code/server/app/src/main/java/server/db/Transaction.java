package server.db;

import java.sql.SQLException;

public class Transaction implements AutoCloseable{
    public DbConnection conn;

    public Transaction(DbConnection conn) throws SQLException {
        conn.conn.setAutoCommit(false);
        this.conn = conn;
    }

    public synchronized void commit() throws SQLException {
        if(conn != null){
            conn.conn.commit();
            conn.conn.setAutoCommit(true);
            conn.close();
            conn = null;
        }else throw new RuntimeException("Transaction has already closed");
    }

    public synchronized void rollback() throws SQLException{
        if(conn != null){
            conn.conn.rollback();
            conn.conn.setAutoCommit(true);
            conn.close();
            conn = null;
        }else throw new RuntimeException("Transaction has already closed");
    }

    @Override
    public synchronized void close() throws SQLException {
        if(conn != null)rollback();
    }

    public synchronized void tryCommit() throws SQLException {
        if(conn != null){
            conn.conn.commit();
            conn.conn.setAutoCommit(true);
            conn.close();
            conn = null;
        }
    }
}
