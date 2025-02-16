package server.web;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class TimedEvents implements Closeable {
    private final Timer timer;
    private final ArrayList<Runnable> minutely = new ArrayList<>();

    public TimedEvents(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                minutely.forEach(Runnable::run);
            }
        }, 0, 60*1000);
    }

    public void addMinutely(Runnable runnable){
        minutely.add(runnable);
    }

    @Override
    public void close() {
        timer.cancel();
        timer.purge();
        timer.cancel();
        timer.purge();
    }
}
