package ru.nsu.vyaznikova;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java -jar d3.jar <xml-file> <db-url> <db-user> <db-password> [threads]");
            System.out.println("Example: java -jar d3.jar people-x2.xml jdbc:postgresql://localhost:5432/family user pass 4");
            System.exit(1);
        }

        String xmlFile = args[0];
        String dbUrl = args[1];
        String dbUser = args[2];
        String dbPass = args[3];
        int threads = args.length > 4 ? Integer.parseInt(args[4]) : 1;

        System.out.println("D3: Loading data from " + xmlFile);
        System.out.println("Threads: " + threads);

        try {
            DatabaseManager db = new DatabaseManager(dbUrl, dbUser, dbPass);
            
            System.out.println("\n=== Phase 1: Streaming ===");
            StreamingLoader loader = new StreamingLoader(db, threads);
            loader.load(xmlFile);
            
            System.out.println("\n=== Phase 2: Normalization ===");
            Normalizer normalizer = new Normalizer(db);
            normalizer.normalize();
            
            db.close();
            System.out.println("\nDone!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}