package server.infrastructure;

import framework.db.DbManager;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteUpdateListener;
import server.Config;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbManagerImpl extends DbManager {
    private final List<SQLiteUpdateListener> listeners = new ArrayList<>();

    public DbManagerImpl() throws SQLException {
        this(Config.CONFIG.db_path, Config.CONFIG.store_db_in_memory, Config.CONFIG.wipe_db_on_start, true);
    }

    public synchronized void addUpdateHook(SQLiteUpdateListener listener){
        listeners.add(listener);
        super.forEachExisting(conn -> {
            ((SQLiteConnection) conn).addUpdateListener(listener);
        });
    }

    public DbManagerImpl(String path, boolean inMemory, boolean alwaysInitialize, boolean cacheShared) throws SQLException {
        super(false, 0, 1);

        boolean createTables;
        if(inMemory) {
            initialize("jdbc:sqlite:file:"+path+"?mode=memory"+(cacheShared?"&cache=shared":""));
            createTables = true;
        }else {
            createTables = !new File(path).exists();
            initialize("jdbc:sqlite:file:"+path+(cacheShared?"?cache=shared":""));
        }

        if(createTables||alwaysInitialize){
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

    public synchronized static String sql(String id){
        String resourcePath = "/sql/"+id+".sql";
        try (var resourceStream = DbManager.class.getResourceAsStream(resourcePath)) {
            var resourceData = Objects.requireNonNull(resourceStream).readAllBytes();
            return new String(resourceData);
        }catch (IOException e){
            Logger.getGlobal().log(Level.SEVERE, "Failed to load resource", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Connection openConnection(boolean readOnly) throws SQLException {
        var config = new SQLiteConfig();
        config.setReadOnly(readOnly);
        config.setPragma(SQLiteConfig.Pragma.SHARED_CACHE, "true");
        config.setPragma(SQLiteConfig.Pragma.JOURNAL_MODE, SQLiteConfig.JournalMode.WAL.getValue());
        config.setPragma(SQLiteConfig.Pragma.READ_UNCOMMITTED, "true");
        config.setPragma(SQLiteConfig.Pragma.FOREIGN_KEYS, "true");
        config.setPragma(SQLiteConfig.Pragma.RECURSIVE_TRIGGERS, "true");
        config.setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, "normal");
        config.setPragma(SQLiteConfig.Pragma.JOURNAL_SIZE_LIMIT, "6144000");
        var connection = (SQLiteConnection) DriverManager.getConnection(url, config.toProperties());
        connection.setCurrentTransactionMode(SQLiteConfig.TransactionMode.DEFERRED);
        connection.setAutoCommit(false);

        synchronized (this){
            for(var listener : listeners){
                connection.addUpdateListener(listener);
            }
        }


        Logger.getGlobal().log(Level.FINE, "New Database Connection Initialized");
        return connection;
    }
}
