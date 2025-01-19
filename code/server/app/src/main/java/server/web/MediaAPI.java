package server.web;

import com.sun.net.httpserver.HttpExchange;
import server.web.annotations.Body;
import server.web.annotations.Route;
import server.web.annotations.http.Get;
import server.web.annotations.http.Post;
import server.web.annotations.url.Path;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

@SuppressWarnings("unused")
public class MediaAPI {

    private static final HashMap<Integer, byte[]> map = new HashMap<>();
    private static final SortedMap<Long, Integer> things = new TreeMap<>();
    private static long cacheSize = 0;
    private static final long maxCacheSize = 1<<22;

    @Route("/get/<id>")
    @Get
    public static byte[] media(HttpExchange exchange, @Path int id){
        return map.getOrDefault(id, new byte[0]);
    }

    @Route
    @Post
    public static int upload(@Body byte[] data){
        map.put(map.size()+1, data);
        return map.size();
    }
}
