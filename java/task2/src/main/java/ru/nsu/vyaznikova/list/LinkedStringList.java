package ru.nsu.vyaznikova.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedStringList implements IterableStringList {
    private final Node head;
    private final Node tail;
    private final AtomicInteger size = new AtomicInteger(0);

    public LinkedStringList() {
        this.head = new Node(null);
        this.tail = new Node(null);
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public void addFirst(String value) {
        if (value == null) throw new IllegalArgumentException("value is null");
        Node n = new Node(value);

        // lock order: head -> first (closer to head first)
        head.lock.lock();
        Node first;
        try {
            first = head.next;
            if (first != null) first.lock.lock();
            try {
                // insert n between head and first
                n.prev = head;
                n.next = first;
                head.next = n;
                if (first != null) first.prev = n;
                size.incrementAndGet();
            } finally {
                if (first != null) first.lock.unlock();
            }
        } finally {
            head.lock.unlock();
        }
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            Node curr;
            {
                // initialize at first real node
                head.lock.lock();
                try {
                    curr = head.next;
                    if (curr == tail) curr = null;
                    if (curr != null) curr.lock.lock();
                } finally {
                    head.lock.unlock();
                }
            }

            @Override
            public boolean hasNext() {
                return curr != null;
            }

            @Override
            public String next() {
                if (curr == null) throw new NoSuchElementException();
                String out = curr.value;
                // prepare advance with lock-coupling: lock next, unlock curr
                Node next = curr.next;
                if (next == tail) next = null;
                if (next != null) next.lock.lock();
                curr.lock.unlock();
                curr = next;
                return out;
            }
        };
    }

    @Override
    public List<String> snapshot() {
        List<String> res = new ArrayList<>(Math.max(0, size()));
        Iterator<String> it = iterator();
        while (it.hasNext()) {
            res.add(it.next());
        }
        return res;
    }


    public int bubblePass(long insideDelayMs, long betweenDelayMs, Runnable stepHook) throws InterruptedException {
        int swaps = 0;
        Node prev = head;

        while (true) {
            Node nextPrev = null; // where prev should move after unlocking

            // lock prev first (closer to head)
            prev.lock.lock();
            try {
                Node a = prev.next;
                if (a == null || a == tail) {
                    return swaps; // list shorter than 2 from this point
                }

                a.lock.lock();
                try {
                    // adjacency check prev->a
                    if (prev.next != a || a.prev != prev) {
                        nextPrev = prev; // retry with same prev
                    } else {
                        Node b = a.next;
                        if (b == null || b == tail) {
                            return swaps; // reached tail
                        }

                        b.lock.lock();
                        try {
                            // adjacency check a->b
                            if (a.next != b || b.prev != a) {
                                nextPrev = prev; // retry with same prev
                            } else {
                                // inside delay within pair lock
                                if (insideDelayMs > 0) Thread.sleep(insideDelayMs);
                                if (stepHook != null) stepHook.run();

                                if (a.value.compareTo(b.value) > 0) {
                                    // swap by relinking: prev <-> b <-> a <-> afterB
                                    Node afterB = b.next; // can be tail

                                    // detach a and b from current order and re-link swapped
                                    prev.next = b;
                                    b.prev = prev;

                                    b.next = a;
                                    a.prev = b;

                                    a.next = afterB;
                                    if (afterB != null) afterB.prev = a;

                                    swaps++;
                                    nextPrev = prev.next; // which is b after swap
                                } else {
                                    // advance without swap
                                    nextPrev = a;
                                }
                            }
                        } finally {
                            b.lock.unlock();
                        }
                    }
                } finally {
                    a.lock.unlock();
                }
            } finally {
                prev.lock.unlock();
            }

            // delay between steps outside locks
            if (betweenDelayMs > 0) Thread.sleep(betweenDelayMs);

            // move prev for next step
            prev = (nextPrev != null) ? nextPrev : head;
        }
    }

    // internal doubly-linked node with its own lock
    private static final class Node {
        final ReentrantLock lock = new ReentrantLock();
        String value;
        Node prev;
        Node next;
        Node(String value) { this.value = value; }
    }
}
