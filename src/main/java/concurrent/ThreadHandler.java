package concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ThreadHandler {

    public void initiateTaskRoutine(boolean isDaemon, AsyncFunction function) {
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(isDaemon);
            return thread;
        });
        singleThreadExecutor.submit(()-> {
            try {
                function.execute();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }


    public ExecutorService initiateServiceThreadPool(int threadCount) {
        return Executors.newFixedThreadPool(threadCount);

    }


}
