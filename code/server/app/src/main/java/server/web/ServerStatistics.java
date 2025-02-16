package server.web;

import com.google.gson.Gson;

import java.util.HashMap;

public class ServerStatistics {
    private final HashMap<String, Stats> route_stats = new HashMap<>();
    private long total_requests_handled = 0;

    private long prepared_statements_executed = 0;
    private long statements_executed = 0;
    private long total_db_statements_executed = 0;

    public final static class Stats{
        public double average_response_time;
        public long requests_handled = 0;
        public final HashMap<Integer, Integer> code_breakdown = new HashMap<>();
    }

    public void track_route(String p, int code, double duration) {
        Stats stat;
        synchronized (route_stats){
            stat = route_stats.computeIfAbsent(p, k -> new Stats());
            total_requests_handled++;
        }
        synchronized (stat){
            stat.requests_handled++;
            if(stat.average_response_time==0)
                stat.average_response_time = duration;
            else
                stat.average_response_time = (stat.average_response_time*99+duration)/100;
            stat.code_breakdown.compute(code, (unused, value) -> value==null?1:value+1);
        }
    }

    public synchronized void executed_prepared_statement(){
        prepared_statements_executed++;
        total_db_statements_executed++;
    }

    public synchronized void executed_statement(){
        statements_executed++;
        total_db_statements_executed++;
    }

    public String json(){
        synchronized (route_stats){
            return new Gson().toJson(this);
        }
    }
}
