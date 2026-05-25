package ru.nsu.vyaznikova;

import jakarta.xml.bind.JAXBException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ru.nsu.vyaznikova.model.People;

public class Main {
    public static void main(String[] args) {
        Path xml = args != null && args.length > 0
                ? Path.of(args[0])
                : Path.of("..", "x1", "src", "main", "resources", "people-normalized.xml");
        Path xsd = Path.of("src", "main", "resources", "people-normalized.xsd");
        Path out = Path.of("src", "main", "resources", "people-x2.xml");
        Path dedupXml = Path.of("src", "main", "resources", "people-deduped.xml");

        ParentDeduplicator dedup = new ParentDeduplicator();
        XmlValidator validator = new XmlValidator();
        try {
            long t0;

            t0 = System.currentTimeMillis();
            dedup.deduplicate(xml, dedupXml);
            long dt = System.currentTimeMillis() - t0;
            System.out.println("Parent deduplication OK. Time: " + dt + " ms");
            System.out.println("Source: " + xml.toAbsolutePath());
            System.out.println("Temp:   " + dedupXml.toAbsolutePath());

            t0 = System.currentTimeMillis();
            validator.validate(dedupXml, xsd);
            dt = System.currentTimeMillis() - t0;
            System.out.println("Validation OK. XML conforms to XSD. Time: " + dt + " ms");
            System.out.println("XML:  " + dedupXml.toAbsolutePath());
            System.out.println("XSD:  " + xsd.toAbsolutePath());

            JaxbIO jaxb = new JaxbIO();
            People people = jaxb.unmarshal(dedupXml, xsd);
            jaxb.marshal(people, out, xsd, true);
            System.out.println("Wrote JAXB-validated XML to: " + out.toAbsolutePath());
        } catch (IOException | SAXException | JAXBException e) {
            System.err.println("Pipeline FAILED: " + e.getMessage());
            System.err.println("XML:  " + xml.toAbsolutePath());
            System.err.println("XSD:  " + xsd.toAbsolutePath());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Pipeline FAILED: " + e.getMessage());
            System.exit(1);
        } finally {
            try {
                Files.deleteIfExists(dedupXml);
            } catch (IOException ignored) {
            }
        }
    }
}