package com.farmermarket.ui.util;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
public final class TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);


    private static final int POOL_SIZE = 4;


    private static final ExecutorService POOL = Executors.newFixedThreadPool(
            POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "farmermarket-worker");
                t.setDaemon(true); // Don't block JVM shutdown
                return t;
            }
    );


    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "farmermarket-scheduler");
                t.setDaemon(true);
                return t;
            });


    private TaskRunner() {}

    public static <T> void run(Task<T> task) {
        POOL.submit(task);
    }


    public static ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }


    public static void shutdown() {
        log.info("Shutting down TaskRunner thread pool and scheduler...");
        POOL.shutdown();
        SCHEDULER.shutdown();
        try {
            if (!POOL.awaitTermination(5, TimeUnit.SECONDS)) {
                POOL.shutdownNow();
            }
            if (!SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            POOL.shutdownNow();
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("TaskRunner shutdown complete.");
    }
}
