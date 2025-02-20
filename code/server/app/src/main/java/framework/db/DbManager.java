package framework.db;


import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbManager implements AutoCloseable{
    private final static HashMap<String, String> resources = new HashMap<>();

    private final LinkedList<Connection> writableAvailable = new LinkedList<>();
    private final LinkedList<Connection> readOnlyAvailable = new LinkedList<>();
    private final HashSet<Connection> inUse = new HashSet<>();
    private final HashSet<Thread> owning = new HashSet<>();
    private int inUseReadOnly = 0;
    private int inUseWritable = 0;
    private int sequenceBefore = 0;
    private int sequenceCurr = 0;

    private final String url;
    private final int maxReadOnly;
    private final int maxWritable;
    private final boolean canWriteAndReadCoexist;

    private final DbStatistics tracker = new DbStatistics();

    public DbManager(String path, boolean inMemory, boolean alwaysInitialize, boolean cacheShared) throws SQLException {

        boolean initialized;


        canWriteAndReadCoexist = false;
        maxReadOnly = 0;
        maxWritable = 1;
        if(inMemory) {
            initialized = false;
            url = "jdbc:sqlite:file:"+path+"?mode=memory"+(cacheShared?"&cache=shared":"");
        }else {
            url = "jdbc:sqlite:file:"+path+(cacheShared?"?cache=shared":"");
            initialized = new File(path).exists();
        }
        Logger.getGlobal().log(Level.CONFIG, "DB url: " + url);

        try(var conn = rw_conn(">initialization")){
            var major = conn.getConn().getMetaData().getDatabaseMajorVersion();
            var minor = conn.getConn().getMetaData().getDatabaseMinorVersion();
            var name = conn.getConn().getMetaData().getDatabaseProductName();
            var v = conn.getConn().getMetaData().getDatabaseProductVersion();
            Logger.getGlobal().log(Level.INFO, "Connected to DB " + major + "." + minor + " " + name + "("+v+")");
        }

        if(!initialized || alwaysInitialize){
            try(var conn = rw_conn(">initialization")){
                Logger.getGlobal().log(Level.FINE, "Initializing DB");

                try(var stmt = conn.createStatement()){
                    for(var sql : sql("creation").split(";")){
                        try{
                            stmt.execute(sql);
                        }catch (SQLException e){
                            Logger.getGlobal().log(Level.WARNING, sql);
                            throw e;
                        }
                    }
                }catch (SQLException e){
                    Logger.getGlobal().log(Level.SEVERE, "Failed to initialize DB", e);
                    throw e;
                }
                conn.commit();
                Logger.getGlobal().log(Level.CONFIG, "Initialized DB");
            }
        }
    }

    public DbStatistics getTracker(){
        return this.tracker;
    }

    public synchronized static String sql(String id){
        var existing = resources.get(id);
        if(existing != null) return existing;
        String resourcePath = "/sql/"+id+".sql";
        try (var resourceStream = DbManager.class.getResourceAsStream(resourcePath)) {
            var resourceData = Objects.requireNonNull(resourceStream).readAllBytes();
            var resource = new String(resourceData);
            resources.put(id, resource);
            return resource;
        }catch (IOException e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to load resource", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void close() {
        for(var conn : readOnlyAvailable){
            try{
                conn.close();
            }catch (Exception ignore){}
        }
        for(var conn : writableAvailable){
            try{
                conn.close();
            }catch (Exception ignore){}
        }
        for(var conn : inUse){
            try{
                conn.close();
            }catch (Exception ignore){}
        }
    }

    protected synchronized void rePool(RwConn conn) throws SQLException {
        if(conn.conn==null)return;
        owning.remove(Thread.currentThread());
        if(inUse.remove(conn.conn)){
            if(!conn.conn.isClosed())
                writableAvailable.addLast(conn.conn);
            inUseWritable--;
            this.notifyAll();
        }else{
            Logger.getGlobal().log(Level.SEVERE, "Tried to re pool connection that wasn't in use??");
        }
        tracker.db_release(conn.id, true, System.nanoTime()-conn.acquired);
    }

    protected synchronized void rePool(RoConn conn) throws SQLException {
        if(conn.conn==null)return;
        owning.remove(Thread.currentThread());
        if(inUse.remove(conn.conn)){
            if(!conn.conn.isClosed())
                readOnlyAvailable.addLast(conn.conn);
            inUseReadOnly--;
            this.notifyAll();
        }else{
            Logger.getGlobal().log(Level.SEVERE, "Tried to re pool connection that wasn't in use??");
        }
        tracker.db_release(conn.id, false, System.nanoTime()-conn.acquired);
    }

    private SQLiteConnection initialize(boolean readOnly) throws SQLException{
        var config = new SQLiteConfig();
        config.setReadOnly(readOnly);
        config.setPragma(SQLiteConfig.Pragma.SHARED_CACHE, "true");
        config.setPragma(SQLiteConfig.Pragma.JOURNAL_MODE, SQLiteConfig.JournalMode.WAL.getValue());
        config.setPragma(SQLiteConfig.Pragma.READ_UNCOMMITTED, "true");
        config.setPragma(SQLiteConfig.Pragma.FOREIGN_KEYS, "true");
        config.setPragma(SQLiteConfig.Pragma.RECURSIVE_TRIGGERS, "true");
        var connection = (SQLiteConnection)DriverManager.getConnection(url, config.toProperties());
        connection.setCurrentTransactionMode(SQLiteConfig.TransactionMode.DEFERRED);
        connection.setAutoCommit(false);

        Logger.getGlobal().log(Level.FINE, "New Database Connection Initialized");
        return connection;
    }

    public RwTransaction rw_transaction(String id) throws SQLException {
        return new RwTransaction(this, id);
    }

    public RoTransaction ro_transaction(String id) throws SQLException {
        return new RoTransaction(this, id);
    }

    public synchronized RoConn ro_conn(String id) throws SQLException {
        return new RoConn(this, id);
    }

    public synchronized RwConn rw_conn(String id) throws SQLException{
        return new RwConn(this, id);
    }

    protected synchronized Connection rw_conn_p(RwConn conn) throws SQLException{
        var seq = sequenceBefore++;

        tracker.db_lock_waited(conn.id, true);
        while(
            seq!=sequenceCurr
            ||(inUseWritable>=maxWritable&&maxWritable>0)
            ||(!canWriteAndReadCoexist&&inUseReadOnly>0)
        ){
            var start = System.nanoTime();
            if(owning.contains(Thread.currentThread()))
                throw new RuntimeException("This is probably going to be a deadlock, This thread already owns a lock that likely would have caused this to block");
            try{
                wait();
            }catch (Exception e){
                throw new SQLException(e);
            }
            tracker.db_lock_waited(conn.id, true, System.nanoTime()-start);
        }
        tracker.db_acquire(conn.id, true);
        conn.acquired = System.nanoTime();
        sequenceCurr++;
        inUseWritable++;
        Connection con;
        if(writableAvailable.isEmpty()){
            con = initialize(false);
        }else{
            con = writableAvailable.pollFirst();
        }
        inUse.add(con);
        owning.add(Thread.currentThread());
        return con;
    }

    protected synchronized Connection ro_conn_p(RoConn conn) throws SQLException {
        var seq = sequenceBefore++;

        tracker.db_lock_waited(conn.id, false);
        while(
            seq!=sequenceCurr
            ||(inUseReadOnly>=maxReadOnly&&maxReadOnly>0)
            ||(!canWriteAndReadCoexist&&inUseWritable>0)
        ){
            var start = System.nanoTime();
            if(owning.contains(Thread.currentThread()))
                throw new RuntimeException("This is probably going to be a deadlock, This thread already owns a lock that likely would have caused this to block");
            try{
                wait();
            }catch (Exception e){
                throw new SQLException(e);
            }
            tracker.db_lock_waited(conn.id, false, System.nanoTime()-start);
        }
        tracker.db_acquire(conn.id, false);
        conn.acquired = System.nanoTime();
        sequenceCurr++;
        inUseReadOnly++;
        Connection con;
        if(readOnlyAvailable.isEmpty()){
            con = initialize(true);
        }else{
            con = readOnlyAvailable.pollFirst();
        }
        inUse.add(con);
        owning.add(Thread.currentThread());
        return con;
    }
}
