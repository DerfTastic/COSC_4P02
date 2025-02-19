package framework.db;

import java.sql.SQLException;

public class RoTransaction extends RoConn implements AutoCloseable{

    public RoTransaction(DbManager db, String id) {
        super(db, id);
    }

    @Override
    protected void initialize() throws SQLException {
        super.initialize();
//        conn.setAutoCommit(false);
    }

    @Override
    public synchronized void commit() throws SQLException {
        if(!isClosed()){
//            getConn().commit();
//            getConn().setAutoCommit(true);
            super.close();
        }else throw new RuntimeException("Transaction has already closed");
    }

    @Override
    public synchronized void rollback() throws SQLException{
        if(!isClosed()){
//            getConn().rollback();
//            getConn().setAutoCommit(true);
            super.close();
        }else throw new RuntimeException("Transaction has already closed");
    }

    @Override
    public synchronized void close() throws SQLException {
//        if(!isClosed())rollback();
        super.close();
    }

    public synchronized void tryCommit() throws SQLException {
        if(!isClosed()){
//            getConn().commit();
//            getConn().setAutoCommit(true);
            super.close();
        }
    }
}
