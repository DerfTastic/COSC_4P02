package server.db;


import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import server.Config;

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
    private final LinkedList<Connection> connections = new LinkedList<>();
    private final HashSet<Connection> outGoing = new HashSet<>();
    private final String url;
    private final int max_connections;

    public DbManager() throws SQLException{
        this(false, Config.CONFIG.wipe_db_on_start);
    }

    public DbManager(boolean inMemory, boolean alwaysInitialize) throws SQLException {

        var initialized = new File("db/database.db").exists();
        if(!new File("db").exists()){
            try {
                Files.createDirectory(Path.of("db"));
            } catch (IOException e) {
                Logger.getGlobal().log(Level.SEVERE, "Cannot create database folder", e);
                throw new RuntimeException(e);
            }
        }

        if(inMemory) {
            initialized = false;
            max_connections = 1;
            url = "jdbc:sqlite:file:memdb1?mode=memory&cache=shared";
        }else {
            max_connections = 0;
            url = "jdbc:sqlite:"+ Config.CONFIG.db_path;
        }

        try(var conn = conn()){
            var major = conn.conn.getMetaData().getDatabaseMajorVersion();
            var minor = conn.conn.getMetaData().getDatabaseMinorVersion();
            var name = conn.conn.getMetaData().getDatabaseProductName();
            var v = conn.conn.getMetaData().getDatabaseProductVersion();
            Logger.getGlobal().log(Level.FINE, "Connected to DB " + major + "." + minor + " " + name + "("+v+")");
        }


        if(!initialized || alwaysInitialize){
            try(var conn = conn()){
                Logger.getGlobal().log(Level.FINE, "Initializing DB");

                try(var stmt = conn.conn.createStatement()){
                    conn.conn.setAutoCommit(true);
                    for(var sql : sql("creation").split(";")){
                        try{
                            stmt.execute(sql);
                        }catch (SQLException e){
                            Logger.getGlobal().log(Level.WARNING, sql);
                            throw e;
                        }
                    }
                    if(Config.CONFIG.initialize_db_with_data)
                        for(var sql : sql("testing_data").split(";")){
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

    public Transaction transaction() throws SQLException {
        return new Transaction(conn());
    }

    @Override
    public synchronized void close() {
        for(var conn : connections){
            try{
                conn.close();
            }catch (Exception ignore){}
        }
        for(var conn : outGoing){
            try{
                conn.close();
            }catch (Exception ignore){}
        }
    }

    protected synchronized void reAddConnection(Connection conn){
        outGoing.remove(conn);
        connections.addLast(conn);
        notify();
    }

    protected synchronized void removeConnection(Connection conn) {
        outGoing.remove(conn);
    }

    private SQLiteConnection initialize() throws SQLException{
        var config = new SQLiteConfig();
        config.setSharedCache(true);
        config.enableRecursiveTriggers(true);
        var connection = (SQLiteConnection)DriverManager.getConnection(url, config.toProperties());
        connection.setCurrentTransactionMode(SQLiteConfig.TransactionMode.DEFERRED);
        connection.createStatement().execute("PRAGMA read_uncommitted=true");
        connection.createStatement().execute("PRAGMA foreign_keys=true");
        connection.createStatement().execute("PRAGMA recursive_triggers=true");

        Logger.getGlobal().log(Level.FINE, "New Database Connection Initialized");
        return connection;
    }

    public synchronized DbConnection conn() throws SQLException {
        DbConnection conn;
        while(connections.isEmpty()&&outGoing.size()>=max_connections&&max_connections>0){
            try{
                wait();
            }catch (Exception e){
                throw new SQLException(e);
            }
        }
        if(connections.isEmpty()){
            conn = new DbConnection(initialize(), this);
        }else{
            conn = new DbConnection(connections.pollFirst(), this);
        }
        outGoing.add(conn.conn);
        return conn;
    }
}
