package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicMediaHandler {

    private long next_id;
    private final String root_path;
    private final HashMap<Long, byte[]> cache = new HashMap<>();
    private long cache_size = 0;
    private final long max_cache_size;


    private final long bit_split;
    private final long mask;
    private final long bits_used;
    private final long sections;
    private final long first_section_start_bit;

    public DynamicMediaHandler(String rp, long cache_size, int section_bits, int bits_used) throws IOException{

        this.bit_split = section_bits;
        this.mask = (1L<<bit_split)-1;
        this.bits_used = bits_used;
        this.sections = (bits_used+bit_split-1)/bit_split;
        this.first_section_start_bit = (sections-1)*bit_split;

        if(rp.endsWith("/")||rp.endsWith("\\"))
            root_path = rp;
        else
            root_path = rp + "/";

        this.max_cache_size = cache_size;

        next_id = 0;
        var buf = new StringBuilder();
        buf.append(root_path);
        while(true){
            var highest = -1;
            var directory = false;
            try(var stream = Files.list(Path.of(buf.toString()))){
                for(var file : (Iterable<Path>)stream::iterator){
                    int value;
                    try{
                        value = Integer.parseInt(file.getFileName().toString().substring(1));
                    }catch (Exception ignore){
                        continue;
                    }
                    if(!directory&&file.getFileName().toString().startsWith("f")){
                        highest = Math.max(value, highest);
                    }else if(file.getFileName().toString().startsWith("d")){
                        if(!directory) {
                            highest = value;
                            directory = true;
                        }else{
                            highest = Math.max(value, highest);
                        }
                    }
                }
            }

            next_id <<= bit_split;
            next_id += highest;
            if(!directory){
                break;
            }else{
                buf.append("d").append(highest).append("/");
            }
        }
        next_id+=1;
        if(next_id==0)next_id=1;

    }

    public DynamicMediaHandler() throws IOException {
        this(Config.CONFIG.dynamic_media_path, Config.CONFIG.dynamic_media_cache_size, 8, 64);
    }

    private String pathFromId(long id, boolean createDirectories) throws IOException {
        var start_bit = first_section_start_bit;
        var builder = new StringBuilder();
        builder.append(root_path);
        while(start_bit>0){
            var part = (id>>>start_bit)&mask;
            builder.append("d").append(part).append("/");
            Path path = Path.of(builder.toString());
            synchronized (root_path){
                if(createDirectories&&Files.notExists(path)){
                    Files.createDirectory(path);
                }
            }
            start_bit-=bit_split;
        }
        builder.append("f").append(id&mask);
        return builder.toString();
    }

    public synchronized byte[] get(long id) {
        synchronized (cache){
            var item = cache.get(id);
            if(item!=null)
                return item;
        }
        try {
            var path = Path.of(pathFromId(id, false));
            synchronized (root_path){
                if(Files.exists(path)){
                    var data = Files.readAllBytes(path);
                    try_add_cached(id, data);
                    return data;
                }
            }
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to get path", e);
        }
        return new byte[0];
    }

    public void delete(long id){
        synchronized (cache){
            var element = cache.remove(id);
            if(element!=null)
                cache_size -= element.length;
        }
        try {
            var path = Path.of(pathFromId(id, false));
            synchronized (root_path){
                if(Files.exists(path)){
                    Files.delete(path);
                }
                for(int i = 0; i < sections-1; i ++){
                    path = path.getParent();
                    try(var list = Files.list(path)){
                        if(list.toList().isEmpty()){
                            Files.delete(path);
                        }else{
                            break;
                        }
                    }

                }
            }
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to get path", e);
        }
    }

    public long add(byte[] data){
        long id;
        synchronized (this){
            id = next_id;
            next_id++;
        }
        try_add_cached(id, data);
        try {
            var path = Path.of(pathFromId(id, true));
            synchronized (root_path){
                if(!Files.exists(path)){
                    Files.write(path, data);
                }else{
                    Logger.getGlobal().log(Level.SEVERE, "Dynamic Media ID conflict!!!!");
                }
            }
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Failed to get path", e);
        }
        return id;
    }

    private void try_add_cached(long id, byte[] data){
        synchronized (cache){
            if(data.length>max_cache_size)return;
            //TODO potentially make this time based/remove whichever cached item that was accessed last
            for(var entry: cache.entrySet()){
                cache.remove(entry.getKey());
                cache_size-=entry.getValue().length;
                if(cache_size+data.length<=max_cache_size)break;
            }
            cache.put(id, data);
        }
    }


}
