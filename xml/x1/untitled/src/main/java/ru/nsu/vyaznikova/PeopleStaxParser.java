package ru.nsu.vyaznikova;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

public class PeopleStaxParser {
    public PeopleRepository parse(InputStream in) throws XMLStreamException {
        PeopleRepository repo = new PeopleRepository();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(in);

        Person current = null;
        String currentElement = null;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = reader.getLocalName();
                currentElement = name;
                if ("person".equals(name)) {
                    current = new Person();
                    String idAttr = getAttr(reader, "id");
                    if (idAttr != null && !idAttr.isEmpty()) {
                        current.setId(idAttr);
                    }
                }
            } else if (event == XMLStreamConstants.CHARACTERS) {
                String text = reader.getText();
                if (current != null && currentElement != null) {
                    if ("firstname".equals(currentElement) || "first".equals(currentElement)) {
                        if (current.getFirstName() == null || current.getFirstName().isEmpty()) {
                            current.setFirstName(text.trim());
                        }
                    } else if ("surname".equals(currentElement) || "family".equals(currentElement)) {
                        if (current.getLastName() == null || current.getLastName().isEmpty()) {
                            current.setLastName(text.trim());
                        }
                    } else if ("gender".equals(currentElement)) {
                        String g = normalizeGender(text);
                        if (g != null) {
                            current.setGender(g);
                        }
                    } else if ("father".equals(currentElement)) {
                        String val = text.trim();
                        if (!val.isEmpty() && !"UNKNOWN".equalsIgnoreCase(val)) {
                            current.getFatherNames().add(val);
                        }
                    } else if ("mother".equals(currentElement)) {
                        String val = text.trim();
                        if (!val.isEmpty() && !"UNKNOWN".equalsIgnoreCase(val)) {
                            current.getMotherNames().add(val);
                        }
                    } else if ("parent".equals(currentElement)) {
                        String val = text.trim();
                        if (!val.isEmpty() && !"UNKNOWN".equalsIgnoreCase(val)) {
                            current.getFatherNames().add(val);
                            current.getMotherNames().add(val);
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String name = reader.getLocalName();
                if ("person".equals(name) && current != null) {
                    if (current.getId() == null || current.getId().isEmpty()) {
                        String synthesized = synthesizeId(current);
                        if (synthesized != null) {
                            current.setId(synthesized);
                        }
                    }
                    if (current.getId() != null) {
                        repo.put(current);
                    }
                    current = null;
                }
                currentElement = null;
            }
        }

        reader.close();
        return repo;
    }

    private static String normalizeGender(String raw) {
        if (raw == null) return null;
        String r = raw.trim().toLowerCase();
        if (r.isEmpty()) return null;
        if ("m".equals(r) || "male".equals(r)) return "male";
        if ("f".equals(r) || "female".equals(r)) return "female";
        return null;
    }

    private static String getAttr(XMLStreamReader reader, String name) {
        String v = reader.getAttributeValue(null, name);
        if (v != null) return v;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (name.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    private static String synthesizeId(Person p) {
        if (p.getFirstName() != null || p.getLastName() != null) {
            String f = p.getFirstName() == null ? "" : p.getFirstName();
            String l = p.getLastName() == null ? "" : p.getLastName();
            String base = (f + "_" + l).trim();
            if (!base.equals("_")) {
                return ("NAME_" + base).replaceAll("\\s+", "_");
            }
        }
        return null;
    }
}
