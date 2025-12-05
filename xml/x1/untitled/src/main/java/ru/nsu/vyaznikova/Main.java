package ru.nsu.vyaznikova;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path input = Path.of("src/main/resources/people.xml");
        if (!Files.exists(input)) {
            System.err.println("Input XML not found: " + input.toAbsolutePath());
            System.err.println("Place people.xml into src/main/resources/people.xml and run again.");
            return;
        }

        try (InputStream in = Files.newInputStream(input)) {
            PeopleStaxParser parser = new PeopleStaxParser();
            PeopleRepository repo = parser.parse(in);
            System.out.println("Parsed persons: " + repo.allPersons().size());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (XMLStreamException e) {
            System.err.println("XML parse error: " + e.getMessage());
        }
    }
}
