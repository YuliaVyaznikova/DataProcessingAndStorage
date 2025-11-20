package ru.nsu.vyaznikova;

import java.net.http.HttpClient;
import java.util.List;
import ru.nsu.vyaznikova.http.HttpFactory;
import ru.nsu.vyaznikova.crawler.Crawler;
import ru.nsu.vyaznikova.cli.Args;

public class Main {
    public static void main(String[] args) {
        Args.Config cfg = Args.parse(args);
        HttpClient http = HttpFactory.create(java.time.Duration.ofSeconds(15));
        System.out.println("Configuration:\n" +
                "  baseUrl=" + cfg.baseUrl() + "\n" +
                "  verbose=" + cfg.verbose());
        if (cfg.verbose()) {
            System.out.println("HttpClient initialized");
        }

        try {
            Crawler crawler = new Crawler(http);
            List<String> messages = crawler.crawl(cfg.baseUrl(), cfg.verbose());
            for (String m : messages) {
                System.out.println(m);
            }
        } catch (Exception e) {
            System.err.println("Request failed: " + e.getMessage());
            System.exit(2);
        }
    }
}