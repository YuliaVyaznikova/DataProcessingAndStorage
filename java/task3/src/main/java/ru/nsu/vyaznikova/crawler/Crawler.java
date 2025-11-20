package ru.nsu.vyaznikova.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.nsu.vyaznikova.json.Json;
import ru.nsu.vyaznikova.model.ResponseDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class Crawler {
    private final HttpClient http;
    private final ObjectMapper mapper;

    public Crawler(HttpClient http) {
        this.http = http;
        this.mapper = Json.mapper();
    }

    public List<String> crawl(URI baseUrl, boolean verbose) throws Exception {
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<String> messages = new ArrayList<>();

        queue.add("/");
        visited.add("/");

        while (!queue.isEmpty()) {
            String path = queue.poll();
            String norm = normalizePath(path);
            URI uri = baseUrl.resolve(norm);
            if (verbose) System.out.println("GET " + uri);

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (verbose) System.out.println("<- status=" + resp.statusCode());
            if (resp.statusCode() != 200 || resp.body() == null) {
                if (resp.statusCode() != 200) {
                    System.err.println("Non-200 status: " + resp.statusCode());
                }
                continue;
            }

            try {
                ResponseDto dto = mapper.readValue(resp.body(), ResponseDto.class);
                if (dto.getMessage() != null) {
                    messages.add(dto.getMessage());
                }
                List<String> succ = dto.getSuccessors();
                if (succ != null) {
                    for (String s : succ) {
                        String next = normalizePath(s);
                        if (next != null && visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            } catch (Exception parseEx) {
                if (verbose) System.err.println("Parse error: " + parseEx.getMessage());
            }
        }

        Collections.sort(messages);
        return messages;
    }

    private static String normalizePath(String p) {
        if (p == null) return null;
        String s = p.trim();
        if (s.isEmpty()) return "/";
        if (!s.startsWith("/")) s = "/" + s;
        return s;
    }
}
