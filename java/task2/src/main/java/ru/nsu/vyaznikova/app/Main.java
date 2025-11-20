package ru.nsu.vyaznikova.app;

import ru.nsu.vyaznikova.list.LinkedStringList;
import ru.nsu.vyaznikova.sort.ArrayListBubbleSorter;
import ru.nsu.vyaznikova.sort.LinkedListBubbleSorter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    private static class Config {
        String mode = "custom_linked";
        int threads = 2;
        long delayBetweenMs = 100;
        long delayInsideMs = 100;
        int maxLen = 80;
    }

    private static Config parseArgs(String[] args) {
        Config c = new Config();
        for (String a : args) {
            if (a.startsWith("--mode=")) c.mode = a.substring("--mode=".length());
            else if (a.startsWith("--threads=")) c.threads = Integer.parseInt(a.substring("--threads=".length()));
            else if (a.startsWith("--delayBetween=")) c.delayBetweenMs = Long.parseLong(a.substring("--delayBetween=".length()));
            else if (a.startsWith("--delayInside=")) c.delayInsideMs = Long.parseLong(a.substring("--delayInside=".length()));
            else if (a.startsWith("--maxLen=")) c.maxLen = Integer.parseInt(a.substring("--maxLen=".length()));
        }
        return c;
    }

    private static List<String> chunk(String s, int maxLen) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < s.length(); i += maxLen) {
            res.add(s.substring(i, Math.min(i + maxLen, s.length())));
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        Config cfg = parseArgs(args);
        System.out.printf("mode=%s sorters=%d delays: between=%dms inside=%dms maxLen=%d%n",
                cfg.mode, cfg.threads, cfg.delayBetweenMs, cfg.delayInsideMs, cfg.maxLen);

        AtomicLong steps = new AtomicLong();
        List<Thread> workers = new ArrayList<>();

        if ("custom_linked".equalsIgnoreCase(cfg.mode)) {
            LinkedStringList list = new LinkedStringList();
            List<LinkedListBubbleSorter> runners = new ArrayList<>();
            for (int i = 0; i < cfg.threads; i++) {
                LinkedListBubbleSorter r = new LinkedListBubbleSorter(list, cfg.delayBetweenMs, cfg.delayInsideMs, steps);
                Thread t = new Thread(r, "linked-sorter-" + i);
                runners.add(r);
                workers.add(t);
                t.start();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        System.out.println("--- list (size=" + list.size() + ", steps=" + steps.get() + ") ---");
                        for (String s : list) System.out.println(s);
                        System.out.println("--- end ---");
                    } else if (":stats".equals(line)) {
                        System.out.println("steps=" + steps.get());
                    } else {
                        List<String> parts = chunk(line, cfg.maxLen);
                        for (int i = parts.size() - 1; i >= 0; i--) list.addFirst(parts.get(i));
                    }
                }
            } finally {
                for (Thread t : workers) t.interrupt();
                for (Thread t : workers) t.join();
            }
        } else if ("arraylist_sync".equalsIgnoreCase(cfg.mode)) {
            List<String> base = new ArrayList<>();
            List<String> sync = Collections.synchronizedList(base);
            List<ArrayListBubbleSorter> runners = new ArrayList<>();
            for (int i = 0; i < cfg.threads; i++) {
                ArrayListBubbleSorter r = new ArrayListBubbleSorter(sync, cfg.delayBetweenMs, cfg.delayInsideMs, steps);
                Thread t = new Thread(r, "array-sorter-" + i);
                runners.add(r);
                workers.add(t);
                t.start();
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        int size;
                        synchronized (sync) { size = sync.size(); }
                        System.out.println("--- list (size=" + size + ", steps=" + steps.get() + ") ---");
                        synchronized (sync) {
                            for (String s : sync) System.out.println(s);
                        }
                        System.out.println("--- end ---");
                    } else if (":stats".equals(line)) {
                        System.out.println("steps=" + steps.get());
                    } else {
                        List<String> parts = chunk(line, cfg.maxLen);
                        for (int i = parts.size() - 1; i >= 0; i--) {
                            synchronized (sync) { sync.add(0, parts.get(i)); }
                        }
                    }
                }
            } finally {
                for (Thread t : workers) t.interrupt();
                for (Thread t : workers) t.join();
            }
        } else {
            System.err.println("unknown mode: " + cfg.mode);
        }
    }
}
