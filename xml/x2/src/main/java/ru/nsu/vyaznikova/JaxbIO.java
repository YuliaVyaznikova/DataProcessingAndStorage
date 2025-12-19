package ru.nsu.vyaznikova;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;
import ru.nsu.vyaznikova.model.People;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JaxbIO {
    private final JAXBContext ctx;

    public JaxbIO() throws JAXBException {
        this.ctx = JAXBContext.newInstance(People.class.getPackage().getName());
    }

    public People unmarshal(Path xml, Path xsd) throws JAXBException, SAXException, IOException {
        requireExists(xml, "XML not found: ");
        requireExists(xsd, "XSD not found: ");
        Unmarshaller u = ctx.createUnmarshaller();
        u.setSchema(loadSchema(xsd));
        return (People) u.unmarshal(xml.toFile());
    }

    public void marshal(People people, Path out, Path xsd, boolean pretty)
            throws JAXBException, SAXException, IOException {
        if (people == null) throw new IllegalArgumentException("people is null");
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Marshaller m = ctx.createMarshaller();
        if (pretty) m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setSchema(loadSchema(xsd));
        m.marshal(people, out.toFile());
    }

    private static Schema loadSchema(Path xsd) throws SAXException {
        SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return f.newSchema(xsd.toFile());
    }

    private static void requireExists(Path p, String msg) throws IOException {
        if (!Files.exists(p)) throw new IOException(msg + p.toAbsolutePath());
    }
}
