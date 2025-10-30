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

    // internal doubly-linked node with its own lock
    private static final class Node {
        final ReentrantLock lock = new ReentrantLock();
        String value;
        Node prev;
        Node next;
        Node(String value) { this.value = value; }
    }
}
