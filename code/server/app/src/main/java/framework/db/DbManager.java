package framework.db;

import framework.util.Tuple;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DbManager implements AutoCloseable{
    private final static ConcurrentHashMap<Connection, ConcurrentHashMap<String, ConcurrentLinkedQueue<NamedPreparedStatement>>> preparedCache = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, Tuple<String, HashMap<String, Integer>>> preparedFieldMapCache = new ConcurrentHashMap<>();


    private final LinkedList<Connection> writableAvailable = new LinkedList<>();
    private final LinkedList<Connection> readOnlyAvailable = new LinkedList<>();
    private final HashSet<Connection> inUse = new HashSet<>();
    private final HashSet<Thread> owning = new HashSet<>();
    private int inUseReadOnly = 0;
    private int inUseWritable = 0;
    private int sequenceBefore = 0;
    private int sequenceCurr = 0;

    protected String url;
    private final int maxReadOnly;
    private final int maxWritable;
    private final boolean canWriteAndReadCoexist;

    private final DbStatistics tracker = new DbStatistics();

    public DbManager(boolean canWriteAndReadCoexist, int maxReadOnly, int maxWritable) throws SQLException {
        this.canWriteAndReadCoexist = canWriteAndReadCoexist;
        this.maxReadOnly = maxReadOnly;
        this.maxWritable = maxWritable;
    }

    protected synchronized void initialize(String url) throws SQLException{
        this.url = url;
        Logger.getGlobal().log(Level.CONFIG, "DB url: " + url);

        try(var conn = rw_conn(">initialization")){
            var major = conn.getConn().getMetaData().getDatabaseMajorVersion();
            var minor = conn.getConn().getMetaData().getDatabaseMinorVersion();
            var name = conn.getConn().getMetaData().getDatabaseProductName();
            var v = conn.getConn().getMetaData().getDatabaseProductVersion();
            Logger.getGlobal().log(Level.INFO, "Connected to DB " + major + "." + minor + " " + name + "("+v+")");
        }
    }
    protected abstract Connection openConnection(boolean readOnly) throws SQLException;

    public DbStatistics getTracker(){
        return this.tracker;
    }

    @Override
    public synchronized void close() {
        for(var conn : readOnlyAvailable){
            try{
                closeConnection(conn);
            }catch (Exception ignore){}
        }
        readOnlyAvailable.clear();
        for(var conn : writableAvailable){
            try{
                closeConnection(conn);
            }catch (Exception ignore){}
        }
        writableAvailable.clear();
        for(var conn : inUse){
            try{
                closeConnection(conn);
            }catch (Exception ignore){}
        }
        inUse.clear();
    }

    protected synchronized void rePool(RwConn conn) throws SQLException {
        if(conn.conn==null)return;
        owning.remove(Thread.currentThread());
        if(inUse.remove(conn.conn)){
            if(conn.conn.isClosed())
                closeConnection(conn.conn);
            else
                writableAvailable.addLast(conn.conn);
            inUseWritable--;
            this.notifyAll();
        }else{
            Logger.getGlobal().log(Level.SEVERE, "Tried to re pool connection that wasn't in use??");
        }
        tracker.db_release(conn.id, true, System.nanoTime()-conn.acquired);
    }

    private void closeConnection(Connection conn) {
        try{
            conn.close();
        }catch (SQLException ignore){}
        for(var stmts : preparedCache.remove(conn).values()){
            for(var stmt : stmts){
                try{
                    stmt.stmt.close();
                }catch (SQLException ignore){}
            }
        }
    }

    protected synchronized void rePool(RoConn conn) throws SQLException {
        if(conn.conn==null)return;
        owning.remove(Thread.currentThread());
        if(inUse.remove(conn.conn)){
            if(conn.conn.isClosed())
                closeConnection(conn.conn);
            else
                readOnlyAvailable.addLast(conn.conn);
            inUseReadOnly--;
            this.notifyAll();
        }else{
            Logger.getGlobal().log(Level.SEVERE, "Tried to re pool connection that wasn't in use??");
        }
        tracker.db_release(conn.id, false, System.nanoTime()-conn.acquired);
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
            con = openConnection(false);
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
            con = openConnection(true);
        }else{
            con = readOnlyAvailable.pollFirst();
        }
        inUse.add(con);
        owning.add(Thread.currentThread());
        return con;
    }


    private static Tuple<String, HashMap<String, Integer>> parsePreparedFields(String sqlO){
        return preparedFieldMapCache.computeIfAbsent(sqlO, sql -> {
            int pos;
            int index = 1;
            final HashMap<String, Integer> fieldMap = new HashMap<>();
            while((pos = sql.indexOf(":")) != -1) {
                var start = pos;
                pos++;
                while(pos < sql.length() && validChar(sql.charAt(pos)))pos++;

                int idx;
                if(fieldMap.containsKey(sql.substring(start,pos))){
                    idx = fieldMap.get(sql.substring(start,pos));
                }else{
                    fieldMap.put(sql.substring(start,pos), index);
                    idx = index;
                    index += 1;
                }
                sql = sql.substring(0, start) + "?" + idx + sql.substring(pos);
            }
            return new Tuple<>(sql, fieldMap);
        });
    }

    private static boolean validChar(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';
    }

    protected NamedPreparedStatement namedPreparedStatement(Conn conn, String sql) throws SQLException{
        var stmt = preparedCache.computeIfAbsent(conn.getConn(), connection -> new ConcurrentHashMap<>())
                .computeIfAbsent(sql, s -> new ConcurrentLinkedQueue<>())
                .poll();
        if(stmt != null){
            stmt.setConn(conn);
            return stmt;
        }
        var res = parsePreparedFields(sql);
        stmt = new NamedPreparedStatement(conn.conn.prepareStatement(res.t1()), sql, res.t1(), res.t2());
        stmt.setConn(conn);
        return stmt;
    }

    public void namedPreparedStatementClose(NamedPreparedStatement namedPreparedStatement) throws SQLException {
        preparedCache.get(namedPreparedStatement.conn.getConn())
                .get(namedPreparedStatement.originalSql)
                .add(namedPreparedStatement);
    }
}
