package ru.nsu.vyaznikova;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XmlValidator {
    public void validate(Path xmlPath, Path xsdPath) throws IOException, SAXException {
        if (!Files.exists(xmlPath)) {
            throw new IOException("XML not found: " + xmlPath.toAbsolutePath());
        }
        if (!Files.exists(xsdPath)) {
            throw new IOException("XSD not found: " + xsdPath.toAbsolutePath());
        }
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(xsdPath.toFile());
        Validator validator = schema.newValidator();
        Source source = new StreamSource(xmlPath.toFile());
        validator.validate(source);
    }
}
