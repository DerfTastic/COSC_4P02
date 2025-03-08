package framework.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DbStatistics {

    private final Stats global = new Stats();
    private final ConcurrentHashMap<String, Stats> individual = new ConcurrentHashMap<>();

    private Stats get_individual(String id){
        return individual.computeIfAbsent(id, s -> new Stats());
    }

    protected void statement_executed(String id, boolean rw){
        if(rw){
            global.rw_statements_executed.incrementAndGet();
            get_individual(id).rw_statements_executed.incrementAndGet();
        }else{
            global.ro_statements_executed.incrementAndGet();
            get_individual(id).ro_statements_executed.incrementAndGet();
        }
    }

    protected void prepared_statement_executed(String id, boolean rw){
        if(rw){
            global.rw_prepared_statements_executed.incrementAndGet();
            get_individual(id).rw_prepared_statements_executed.incrementAndGet();
        }else{
            global.ro_prepared_statements_executed.incrementAndGet();
            get_individual(id).ro_prepared_statements_executed.incrementAndGet();
        }
    }

    protected void db_acquire(String id, boolean rw){
        if(rw){
            global.rw_db_acquires.incrementAndGet();
            get_individual(id).rw_db_acquires.incrementAndGet();
        }else{
            global.ro_db_acquires.incrementAndGet();
            get_individual(id).ro_db_acquires.incrementAndGet();
        }
    }

    protected void db_release(String id, boolean rw, long held_ns){
        if(rw){
            global.rw_db_releases.incrementAndGet();
            get_individual(id).rw_db_releases.incrementAndGet();

            global.rw_db_lock_held_ns.addAndGet(held_ns);
            get_individual(id).rw_db_lock_held_ns.addAndGet(held_ns);
        }else{
            global.ro_db_releases.incrementAndGet();
            get_individual(id).ro_db_releases.incrementAndGet();

            global.ro_db_lock_held_ns.addAndGet(held_ns);
            get_individual(id).ro_db_lock_held_ns.addAndGet(held_ns);
        }
    }

    protected void db_lock_waited(String id, boolean rw){
        if(rw){
            global.rw_db_lock_waited.incrementAndGet();
            get_individual(id).rw_db_lock_waited.incrementAndGet();
        }else{
            global.ro_db_lock_waited.incrementAndGet();
            get_individual(id).ro_db_lock_waited.incrementAndGet();
        }
    }

    protected void db_lock_waited(String id, boolean rw, long time_ns){
        if(rw){
            global.rw_db_lock_waited_ns.addAndGet(time_ns);
            get_individual(id).rw_db_lock_waited_ns.addAndGet(time_ns);
        }else{
            global.ro_db_lock_waited_ns.addAndGet(time_ns);
            get_individual(id).ro_db_lock_waited_ns.addAndGet(time_ns);
        }
    }

    public static class Stats{
        private final AtomicLong rw_statements_executed = new AtomicLong(0);
        private final AtomicLong ro_statements_executed = new AtomicLong(0);

        private final AtomicLong rw_prepared_statements_executed = new AtomicLong(0);
        private final AtomicLong ro_prepared_statements_executed = new AtomicLong(0);

        private final AtomicLong rw_db_acquires = new AtomicLong(0);
        private final AtomicLong ro_db_acquires = new AtomicLong(0);

        private final AtomicLong rw_db_releases = new AtomicLong(0);
        private final AtomicLong ro_db_releases = new AtomicLong(0);

        private final AtomicLong rw_db_lock_waited = new AtomicLong(0);
        private final AtomicLong ro_db_lock_waited = new AtomicLong(0);

        private final AtomicLong rw_db_lock_waited_ns = new AtomicLong(0);
        private final AtomicLong ro_db_lock_waited_ns = new AtomicLong(0);

        private final AtomicLong rw_db_lock_held_ns = new AtomicLong(0);
        private final AtomicLong ro_db_lock_held_ns = new AtomicLong(0);
    }
}
