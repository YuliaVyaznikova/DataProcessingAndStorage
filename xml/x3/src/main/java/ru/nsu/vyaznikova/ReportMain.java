package ru.nsu.vyaznikova;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReportMain {
    public static void main(String[] args) throws Exception {
        Path input = args != null && args.length > 0
                ? Path.of(args[0])
                : Path.of("..", "x2", "src", "main", "resources", "people-x2.xml");
        Path xsl = args != null && args.length > 1
                ? Path.of(args[1])
                : Path.of("src", "main", "resources", "report.xsl");
        Path out = args != null && args.length > 2
                ? Path.of(args[2])
                : Path.of("src", "main", "resources", "report.html");

        if (!Files.exists(input)) throw new IllegalArgumentException("Input XML not found: " + input.toAbsolutePath());
        if (!Files.exists(xsl)) throw new IllegalArgumentException("XSL not found: " + xsl.toAbsolutePath());
        if (out.getParent() != null) Files.createDirectories(out.getParent());

        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslSource = new StreamSource(xsl.toFile());
        Transformer transformer = factory.newTransformer(xslSource);
        Source xmlSource = new StreamSource(input.toFile());
        transformer.transform(xmlSource, new StreamResult(out.toFile()));

        System.out.println("Report written to: " + out.toAbsolutePath());
        System.out.println("Input:  " + input.toAbsolutePath());
        System.out.println("XSLT:   " + xsl.toAbsolutePath());
    }
}
