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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbManagerImpl extends DbManager {
    private final List<SQLiteUpdateListener> listeners = new ArrayList<>();

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


                var error = new AtomicBoolean(false);
                try(var stmt = conn.createStatement()){
                    sql_split(sql("creation"), statement -> {
                        try{
                            stmt.execute(statement);
                        }catch (SQLException e){
                            Logger.getGlobal().log(Level.WARNING, statement, e);
                            error.set(true);
                        }
                    });
                }catch (SQLException e){
                    Logger.getGlobal().log(Level.SEVERE, "Failed to initialize DB", e);
                    throw e;
                }
                if(error.get())
                    throw new RuntimeException();
                conn.commit();
                Logger.getGlobal().log(Level.CONFIG, "Initialized DB");
            }
        }
    }

    /**
     * Splits a large list of SQL statements into whole SQL statemets which can be executed
     * takes into account begin and end blocks.
     */
    private static void sql_split(String sql, Consumer<String> consumer){
        new Object(){
            char[] chars = sql.toCharArray();
            int index = 0;

            int c(){
                if(index < chars.length)
                    return chars[index];
                else
                    return -1;
            }
            int c(int ahead){
                if(index+ahead < chars.length)
                    return chars[index+ahead];
                else
                    return -1;
            }
            boolean nextIs(String str){
                var c = str.toCharArray();
                for(int i = 0; i < c.length; i ++){
                    if(c(i)!=c[i])
                        return false;
                }
                return true;
            }
            {
                int begins = 0;
                StringBuilder builder = new StringBuilder();
                while(index < chars.length){
                    if(nextIs("--")){
                        while(c()!='\n')index++;
                        index++;
                    }else if(nextIs("/*")){
                        while(!nextIs("*/"))index++;
                        index++;
                        index++;
                    }else if(c()=='"'||c()=='\''){
                        var now = c();
                        builder.append((char)now);
                        index++;
                        while(index < chars.length&&c()!=now){
                            builder.append((char)c());
                            if(c()=='\\'){
                                index++;
                                builder.append((char)c());
                            }
                            index++;
                        }
                        builder.append((char)c());
                        index++;
                    }else if(nextIs("begin")|nextIs("BEGIN")) {
                        index+=5;
                        begins++;
                        builder.append("BEGIN");
                    }else if(nextIs("end")|nextIs("END")) {
                        index+=3;
                        begins--;
                        builder.append("END");
                    }else if(nextIs(";")&&begins==0){
                        var str = builder.toString();
                        consumer.accept(str.trim());
                        builder.delete(0, builder.length());
                        index++;
                    }else{
                        builder.append((char)c());
                        index++;
                    }
                }
            }
        };
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
    protected SQLiteConnection openConnection(boolean readOnly) throws SQLException {
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
