package framework.web;

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

    public void addAtRate(Runnable runnable, long period_ms){
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, 0, period_ms);
    }

    @Override
    public void close() {
        timer.cancel();
        timer.purge();
        timer.cancel();
        timer.purge();
    }
}
