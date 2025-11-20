package ru.nsu.vyaznikova;

import java.net.http.HttpClient;
import java.util.List;
import ru.nsu.vyaznikova.http.HttpFactory;
import ru.nsu.vyaznikova.crawler.AsyncCrawler;
import ru.nsu.vyaznikova.cli.Args;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Args.Config cfg = Args.parse(args);
        HttpClient http = HttpFactory.create(Duration.ofSeconds(15));
        System.out.println("Configuration:\n" +
                "  baseUrl=" + cfg.baseUrl() + "\n" +
                "  verbose=" + cfg.verbose() + "\n" +
                "  maxConcurrency=" + cfg.maxConcurrency() + "\n" +
                "  totalTimeoutSec=" + cfg.totalTimeoutSec() + (cfg.outPath() != null ? "\n  out=" + cfg.outPath() : ""));
        if (cfg.verbose()) {
            System.out.println("HttpClient initialized");
        }

        try {
            AsyncCrawler crawler = new AsyncCrawler(http);
            List<String> messages = crawler.crawl(
                    cfg.baseUrl(),
                    cfg.verbose(),
                    cfg.maxConcurrency(),
                    Duration.ofSeconds(cfg.totalTimeoutSec()));
            if (cfg.outPath() != null) {
                Path p = Path.of(cfg.outPath());
                Files.write(p, messages);
                System.out.println("Written " + messages.size() + " messages to " + p.toAbsolutePath());
            } else {
                for (String m : messages) {
                    System.out.println(m);
                }
            }
        } catch (Exception e) {
            System.err.println("Request failed: " + e.getMessage());
            System.exit(2);
        }
    }
}