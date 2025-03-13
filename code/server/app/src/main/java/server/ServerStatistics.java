package server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import framework.db.DbStatistics;

import java.util.HashMap;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class ServerStatistics {
    private final HashMap<String, RouteStats> route_stats = new HashMap<>();
    private long total_requests_handled = 0;

    private final DbStatistics db_stats;

    private long curr_time_ms;
    private long max_mem;
    private long total_mem;
    private long free_mem;

    public ServerStatistics(DbStatistics db_stats){
        this.db_stats = db_stats;
    }

    public final static class RouteStats {
        public long total_response_time_ns;
        public long requests_handled = 0;
        public final HashMap<Integer, Integer> code_breakdown = new HashMap<>();
    }

    public void track_route(String p, int code, long response_time_ns) {
        RouteStats stat;
        synchronized (route_stats){
            stat = route_stats.computeIfAbsent(p, k -> new RouteStats());
            total_requests_handled++;
        }
        synchronized (stat){
            stat.requests_handled++;
            stat.total_response_time_ns += response_time_ns;
            stat.code_breakdown.compute(code, (unused, value) -> value==null?1:value+1);
        }
    }

    public synchronized byte[] json(){
        curr_time_ms = System.currentTimeMillis();
        max_mem = Runtime.getRuntime().maxMemory();
        total_mem = Runtime.getRuntime().totalMemory();
        free_mem = Runtime.getRuntime().freeMemory();
        return JSON.toJSONString(this, JSONWriter.Feature.WriteNonStringKeyAsString, JSONWriter.Feature.FieldBased).getBytes();
    }
}
