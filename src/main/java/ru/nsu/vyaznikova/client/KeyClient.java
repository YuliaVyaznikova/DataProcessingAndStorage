package ru.nsu.vyaznikova.client;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

public class KeyClient implements Callable<Integer> {

    private String host;
    private int port;
    private String name;
    private int delaySeconds = 0;
    private boolean abort;
    private Path outDir = Path.of(".");

    private static void printUsage() {
        System.out.println("Usage: key-client --host <host> --port <port> --name <name> [--delay <sec>] [--abort] [--out <dir>]");
        System.out.println("Options:");
        System.out.println("  -h, --host    Server host or DNS name (required)");
        System.out.println("  -p, --port    Server TCP port (required)");
        System.out.println("  -n, --name    Client name (ASCII), terminated by NUL on the wire (required)");
        System.out.println("  -d, --delay   Delay in seconds before reading the response (default: 0)");
        System.out.println("  -a, --abort   Abort after sending the name (do not read the response)");
        System.out.println("  -o, --out     Output directory to save .key and .crt files (default: .)");
    }

    private static KeyClient parseArgs(String[] args) {
        KeyClient c = new KeyClient();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "-h":
                case "--host":
                    if (i + 1 >= args.length) { System.err.println("--host requires a value"); printUsage(); System.exit(2); }
                    c.host = args[++i];
                    break;
                case "-p":
                case "--port":
                    if (i + 1 >= args.length) { System.err.println("--port requires a value"); printUsage(); System.exit(2); }
                    try { c.port = Integer.parseInt(args[++i]); }
                    catch (NumberFormatException ex) { System.err.println("--port must be an integer"); System.exit(2); }
                    break;
                case "-n":
                case "--name":
                    if (i + 1 >= args.length) { System.err.println("--name requires a value"); printUsage(); System.exit(2); }
                    c.name = args[++i];
                    break;
                case "-d":
                case "--delay":
                    if (i + 1 >= args.length) { System.err.println("--delay requires a value"); printUsage(); System.exit(2); }
                    try { c.delaySeconds = Integer.parseInt(args[++i]); }
                    catch (NumberFormatException ex) { System.err.println("--delay must be an integer"); System.exit(2); }
                    break;
                case "-a":
                case "--abort":
                    c.abort = true;
                    break;
                case "-o":
                case "--out":
                    if (i + 1 >= args.length) { System.err.println("--out requires a value"); printUsage(); System.exit(2); }
                    c.outDir = Paths.get(args[++i]);
                    break;
                case "-?":
                case "-help":
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    System.err.println("Unknown option: " + a);
                    printUsage();
                    System.exit(2);
            }
        }
        if (c.host == null || c.port == 0 || c.name == null) {
            System.err.println("Missing required options");
            printUsage();
            System.exit(2);
        }
        return c;
    }

    @Override
    public Integer call() {
        System.out.println("[KeyClient] Command-line parameters:");
        System.out.printf("  host = %s%n", host);
        System.out.printf("  port = %d%n", port);
        System.out.printf("  name = %s%n", name);
        System.out.printf("  delaySeconds = %d%n", delaySeconds);
        System.out.printf("  abort = %s%n", abort);
        System.out.printf("  outDir = %s%n", outDir.toAbsolutePath());
        return 0;
    }

    public static void main(String[] args) {
        KeyClient client = parseArgs(args);
        int exit = 0;
        try {
            exit = client.call();
        } catch (Exception e) {
            e.printStackTrace();
            exit = 1;
        }
        System.exit(exit);
    }
}
