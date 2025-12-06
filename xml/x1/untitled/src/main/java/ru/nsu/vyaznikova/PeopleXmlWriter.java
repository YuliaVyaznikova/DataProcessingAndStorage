package ru.nsu.vyaznikova;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PeopleXmlWriter {
    public void write(Path outputFile, Iterable<Person> people) throws IOException, XMLStreamException {
        try (OutputStream os = Files.newOutputStream(outputFile)) {
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            XMLStreamWriter w = f.createXMLStreamWriter(os, "UTF-8");
            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("people");

            for (Person p : people) {
                w.writeStartElement("person");
                if (p.getId() != null) w.writeAttribute("id", p.getId());

                if (p.getFirstName() != null) {
                    w.writeStartElement("first-name");
                    w.writeCharacters(p.getFirstName());
                    w.writeEndElement();
                }
                if (p.getLastName() != null) {
                    w.writeStartElement("last-name");
                    w.writeCharacters(p.getLastName());
                    w.writeEndElement();
                }
                if (p.getGender() != null) {
                    w.writeStartElement("gender");
                    w.writeCharacters(p.getGender());
                    w.writeEndElement();
                }

                if (p.getSpouseId() != null || p.getSpouseName() != null) {
                    w.writeStartElement("spouse");
                    if (p.getSpouseId() != null) w.writeAttribute("ref", p.getSpouseId());
                    if (p.getSpouseName() != null) w.writeAttribute("name", p.getSpouseName());
                    w.writeEndElement();
                }

                if (!p.getFatherIds().isEmpty() || !p.getFatherNames().isEmpty() || !p.getMotherIds().isEmpty() || !p.getMotherNames().isEmpty()) {
                    w.writeStartElement("parents");
                    for (String id : p.getFatherIds()) {
                        w.writeEmptyElement("father");
                        w.writeAttribute("ref", id);
                    }
                    for (String name : p.getFatherNames()) {
                        w.writeEmptyElement("father");
                        w.writeAttribute("name", name);
                    }
                    for (String id : p.getMotherIds()) {
                        w.writeEmptyElement("mother");
                        w.writeAttribute("ref", id);
                    }
                    for (String name : p.getMotherNames()) {
                        w.writeEmptyElement("mother");
                        w.writeAttribute("name", name);
                    }
                    w.writeEndElement();
                }

                if (!p.getSonIds().isEmpty() || !p.getDaughterIds().isEmpty()) {
                    w.writeStartElement("children");
                    for (String id : p.getSonIds()) {
                        w.writeEmptyElement("son");
                        w.writeAttribute("ref", id);
                    }
                    for (String id : p.getDaughterIds()) {
                        w.writeEmptyElement("daughter");
                        w.writeAttribute("ref", id);
                    }
                    w.writeEndElement();
                }

                if (!p.getBrotherIds().isEmpty() || !p.getSisterIds().isEmpty()) {
                    w.writeStartElement("siblings");
                    for (String id : p.getBrotherIds()) {
                        w.writeEmptyElement("brother");
                        w.writeAttribute("ref", id);
                    }
                    for (String id : p.getSisterIds()) {
                        w.writeEmptyElement("sister");
                        w.writeAttribute("ref", id);
                    }
                    w.writeEndElement();
                }

                w.writeEndElement();
            }

            w.writeEndElement();
            w.writeEndDocument();
            w.flush();
            w.close();
        }
    }
}
