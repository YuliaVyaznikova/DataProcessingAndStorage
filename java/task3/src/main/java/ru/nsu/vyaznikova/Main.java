package ru.nsu.vyaznikova;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.nsu.vyaznikova.model.ResponseDto;

public class Main {
    record Config(URI baseUrl, boolean verbose) {}

    private static void printHelp() {
        System.out.println("Usage: java -jar task3.jar [options]\n" +
                "Options:\n" +
                "  --baseUrl <url>   Base server URL (default: http://localhost:8080)\n" +
                "  --verbose|-v      Verbose logging\n" +
                "  --help|-h         Show this help and exit");
    }

    private static void die(String msg) {
        System.err.println(msg);
        System.err.println();
        printHelp();
        System.exit(1);
    }

    private static Config parseArgs(String[] args) {
        URI baseUrl = URI.create("http://localhost:8080");
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--help":
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
                case "--verbose":
                case "-v":
                    verbose = true;
                    break;
                case "--baseUrl":
                    if (i + 1 >= args.length) die("Missing value for --baseUrl");
                    String v = args[++i];
                    try {
                        baseUrl = new URI(v);
                    } catch (URISyntaxException e) {
                        die("Invalid --baseUrl: " + v);
                    }
                    break;
                default:
                    die("Unknown option: " + a);
            }
        }
        return new Config(baseUrl, verbose);
    }

    public static void main(String[] args) {
        Config cfg = parseArgs(args);
        HttpClient http = createHttpClient();
        System.out.println("Configuration:\n" +
                "  baseUrl=" + cfg.baseUrl + "\n" +
                "  verbose=" + cfg.verbose);
        if (cfg.verbose) {
            System.out.println("HttpClient initialized");
        }

        try {
            URI root = cfg.baseUrl.resolve("/");
            if (cfg.verbose) System.out.println("GET " + root);
            HttpRequest req = HttpRequest.newBuilder(root)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (cfg.verbose) System.out.println("<- status=" + resp.statusCode());
            if (resp.statusCode() == 200) {
                String body = resp.body();
                if (body == null) {
                    System.out.println("");
                } else {
                    ObjectMapper mapper = new ObjectMapper()
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    ResponseDto dto = mapper.readValue(body, ResponseDto.class);
                    System.out.println("message: " + dto.getMessage());
                    System.out.println("successors: " + (dto.getSuccessors() == null ? 0 : dto.getSuccessors().size()));
                }
            } else {
                System.err.println("Non-200 status: " + resp.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Request failed: " + e.getMessage());
            System.exit(2);
        }
    }

    private static HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }
}