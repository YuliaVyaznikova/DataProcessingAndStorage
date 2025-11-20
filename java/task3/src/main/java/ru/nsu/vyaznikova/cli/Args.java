package ru.nsu.vyaznikova.cli;

import java.net.URI;
import java.net.URISyntaxException;

public final class Args {
    private Args() {}

    public static record Config(URI baseUrl, boolean verbose) {}

    public static void printHelp() {
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

    public static Config parse(String[] args) {
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
}
