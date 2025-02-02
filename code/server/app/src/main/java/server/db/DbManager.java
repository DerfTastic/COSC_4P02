package server.db;


import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import server.Config;
import server.web.ServerStatistics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private int inUseReadOnly = 0;
    private int inUseWritable = 0;
    private int sequenceBefore = 0;
    private int sequenceCurr = 0;

    private final String url;
    private final int maxReadOnly;
    private final int maxWritable;
    private final boolean canWriteAndReadCoexist;

    private ServerStatistics stats;

    public DbManager() throws SQLException{
        this(Config.CONFIG.store_db_in_memory, Config.CONFIG.wipe_db_on_start);
    }

    public DbManager(boolean inMemory, boolean alwaysInitialize) throws SQLException {

        boolean initialized;


        canWriteAndReadCoexist = false;
        maxReadOnly = 0;
        maxWritable = 1;
        if(inMemory) {
            initialized = false;
            url = "jdbc:sqlite:file:memdb1?mode=memory&cache=shared";
        }else {
            url = "jdbc:sqlite:"+ Config.CONFIG.db_path;
            initialized = new File("Config.CONFIG.db_path").exists();

            if(!new File("db").exists()){
                try {
                    Files.createDirectory(Path.of("db"));
                } catch (IOException e) {
                    Logger.getGlobal().log(Level.SEVERE, "Cannot create database folder", e);
                    throw new RuntimeException(e);
                }
            }
        }

        try(var conn = rw_conn()){
            var major = conn.getConn().getMetaData().getDatabaseMajorVersion();
            var minor = conn.getConn().getMetaData().getDatabaseMinorVersion();
            var name = conn.getConn().getMetaData().getDatabaseProductName();
            var v = conn.getConn().getMetaData().getDatabaseProductVersion();
            Logger.getGlobal().log(Level.FINE, "Connected to DB " + major + "." + minor + " " + name + "("+v+")");
        }

        if(!initialized || alwaysInitialize){
            try(var conn = rw_conn()){
                Logger.getGlobal().log(Level.FINE, "Initializing DB");

                try(var stmt = conn.createStatement()){
                    conn.getConn().setAutoCommit(true);
                    for(var sql : sql("creation").split(";")){
                        try{
                            stmt.execute(sql);
                        }catch (SQLException e){
                            Logger.getGlobal().log(Level.WARNING, sql);
                            throw e;
                        }
                    }
                }catch (SQLException e){
                    Logger.getGlobal().log(Level.FINE, "Failed to initialize DB", e);
                    throw e;
                }
                Logger.getGlobal().log(Level.FINE, "Initialized DB");
            }
        }
    }

    public void setStatsTracker(ServerStatistics stats){
        this.stats = stats;
    }

    public ServerStatistics getStatsTracker(){
        return this.stats;
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
        if(inUse.remove(conn.conn)){
            if(!conn.conn.isClosed())
                writableAvailable.addLast(conn.conn);
            inUseWritable--;
            this.notifyAll();
        }else{
            Logger.getGlobal().log(Level.SEVERE, "Tried to re pool connection that wasn't in use??");
        }
    }

    protected synchronized void rePool(RoConn conn) throws SQLException {
        if(conn.conn==null)return;
        if(inUse.remove(conn.conn)){
            if(!conn.conn.isClosed())
                readOnlyAvailable.addLast(conn.conn);
            inUseReadOnly--;
            this.notifyAll();
        }else{
            Logger.getGlobal().log(Level.SEVERE, "Tried to re pool connection that wasn't in use??");
        }
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

        Logger.getGlobal().log(Level.FINE, "New Database Connection Initialized");
        return connection;
    }

    public RwTransaction rw_transaction() throws SQLException {
        return new RwTransaction(rw_conn_p(), this);
    }

    public RoTransaction ro_transaction() throws SQLException {
        return new RoTransaction(ro_conn_p(), this);
    }

    public synchronized RoConn ro_conn() throws SQLException {
        return new RoConn(ro_conn_p(), this);
    }

    public synchronized RwConn rw_conn() throws SQLException{
        return new RwConn(rw_conn_p(), this);
    }

    private synchronized Connection rw_conn_p() throws SQLException{
        var seq = sequenceBefore++;
        while(
            seq!=sequenceCurr
            ||(inUseWritable>=maxWritable&&maxWritable>0)
            ||(!canWriteAndReadCoexist&&inUseReadOnly>0)
        ){
            try{
                wait();
            }catch (Exception e){
                throw new SQLException(e);
            }
        }
        sequenceCurr++;
        inUseWritable++;
        Connection con;
        if(writableAvailable.isEmpty()){
            con = initialize(false);
        }else{
            con = writableAvailable.pollFirst();
        }
        inUse.add(con);
        if(inUseWritable!=1)throw new RuntimeException("aflksdkljsdfjkladflk: "+ inUseWritable);
        if(inUseReadOnly!=0)throw new RuntimeException("asdlkajsdflffffffffffffffffffff");
        return con;
    }

    public synchronized Connection ro_conn_p() throws SQLException {
        var seq = sequenceBefore++;
        while(
            seq!=sequenceCurr
            ||(inUseReadOnly>=maxReadOnly&&maxReadOnly>0)
            ||(!canWriteAndReadCoexist&&inUseWritable>0)
        ){
            try{
                wait();
            }catch (Exception e){
                throw new SQLException(e);
            }
        }
        sequenceCurr++;
        inUseReadOnly++;
        Connection con;
        if(readOnlyAvailable.isEmpty()){
            con = initialize(true);
        }else{
            con = readOnlyAvailable.pollFirst();
        }
        inUse.add(con);
        if(inUseWritable>0)throw new RuntimeException("hih?????????");
        return con;
    }
}
