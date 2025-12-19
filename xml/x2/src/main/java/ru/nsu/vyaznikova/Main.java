package ru.nsu.vyaznikova;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path xml = args != null && args.length > 0
                ? Path.of(args[0])
                : Path.of("..", "x1", "src", "main", "resources", "people-normalized.xml");
        Path xsd = Path.of("src", "main", "resources", "people-normalized.xsd");

        XmlValidator validator = new XmlValidator();
        try {
            long t0 = System.currentTimeMillis();
            validator.validate(xml, xsd);
            long dt = System.currentTimeMillis() - t0;
            System.out.println("Validation OK. XML conforms to XSD. Time: " + dt + " ms");
            System.out.println("XML:  " + xml.toAbsolutePath());
            System.out.println("XSD:  " + xsd.toAbsolutePath());
        } catch (IOException | SAXException e) {
            System.err.println("Validation FAILED: " + e.getMessage());
            System.err.println("XML:  " + xml.toAbsolutePath());
            System.err.println("XSD:  " + xsd.toAbsolutePath());
            System.exit(1);
        }
    }
}
