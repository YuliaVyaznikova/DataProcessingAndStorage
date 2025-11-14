package ru.nsu.vyaznikova.sort;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ArrayListBubbleSorter implements Runnable {
    private final List<String> list;
    private final long delayBetweenMs;
    private final long delayInsideMs;
    private final AtomicLong stepCounter;
    private volatile boolean running = true;

    public ArrayListBubbleSorter(List<String> synchronizedList,
                                 long delayBetweenMs,
                                 long delayInsideMs,
                                 AtomicLong stepCounter) {
        this.list = synchronizedList;
        this.delayBetweenMs = delayBetweenMs;
        this.delayInsideMs = delayInsideMs;
        this.stepCounter = stepCounter;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        try {
            while (running) {
                int n;
                synchronized (list) { n = list.size(); }
                for (int i = 0; i < Math.max(0, n - 1) && running; i++) {
                    if (delayInsideMs > 0) TimeUnit.MILLISECONDS.sleep(delayInsideMs);

                    synchronized (list) {
                        if (i + 1 >= list.size()) break;
                        String a = list.get(i);
                        String b = list.get(i + 1);
                        if (a.compareTo(b) > 0) Collections.swap(list, i, i + 1);
                    }
                    stepCounter.incrementAndGet();

                    if (delayBetweenMs > 0) TimeUnit.MILLISECONDS.sleep(delayBetweenMs);
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
