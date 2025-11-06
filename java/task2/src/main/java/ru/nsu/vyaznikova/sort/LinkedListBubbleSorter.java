package ru.nsu.vyaznikova.sort;

import ru.nsu.vyaznikova.list.LinkedStringList;
import java.util.concurrent.atomic.AtomicLong;

public final class LinkedListBubbleSorter implements Runnable {
    private final LinkedStringList list;
    private final long delayBetweenMs;
    private final long delayInsideMs;
    private final AtomicLong stepCounter;
    private volatile boolean running = true;

    public LinkedListBubbleSorter(LinkedStringList list, long delayBetweenMs, long delayInsideMs, AtomicLong stepCounter) {
        this.list = list;
        this.delayBetweenMs = delayBetweenMs;
        this.delayInsideMs = delayInsideMs;
        this.stepCounter = stepCounter;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try {
            while (running) {
                list.bubblePass(delayInsideMs, delayBetweenMs, stepCounter::incrementAndGet);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}