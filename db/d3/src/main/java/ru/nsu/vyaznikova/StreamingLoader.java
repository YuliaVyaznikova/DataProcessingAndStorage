package ru.nsu.vyaznikova;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.*;
import java.util.concurrent.*;

public class StreamingLoader {
    private final DatabaseManager db;
    private final int threads;
    private ExecutorService executor;
    private BlockingQueue<Person> queue;

    public StreamingLoader(DatabaseManager db, int threads) {
        this.db = db;
        this.threads = threads;
    }

    public void load(String xmlFile) throws Exception {
        db.createTempSchema();

        queue = new LinkedBlockingQueue<>(1000);
        executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    Connection conn = db.getConnection();
                    String sql = """
                        INSERT INTO temp.person (id, first_name, last_name, gender, spouse_id, father_id, mother_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO UPDATE SET
                            first_name = COALESCE(EXCLUDED.first_name, temp.person.first_name),
                            last_name = COALESCE(EXCLUDED.last_name, temp.person.last_name),
                            gender = COALESCE(EXCLUDED.gender, temp.person.gender),
                            spouse_id = COALESCE(EXCLUDED.spouse_id, temp.person.spouse_id),
                            father_id = COALESCE(EXCLUDED.father_id, temp.person.father_id),
                            mother_id = COALESCE(EXCLUDED.mother_id, temp.person.mother_id)
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        Person p;
                        while ((p = queue.take()) != Person.POISON) {
                            ps.setString(1, p.id);
                            ps.setString(2, p.firstName);
                            ps.setString(3, p.lastName);
                            ps.setString(4, p.gender);
                            ps.setString(5, p.spouseId);
                            ps.setString(6, p.fatherId);
                            ps.setString(7, p.motherId);
                            ps.executeUpdate();
                        }
                        conn.commit();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        PersonHandler handler = new PersonHandler(queue);
        
        try (InputStream is = new FileInputStream(xmlFile)) {
            parser.parse(is, handler);
        }

        for (int i = 0; i < threads; i++) {
            queue.put(Person.POISON);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        
        System.out.println("Streaming phase completed");
    }

    private static class PersonHandler extends DefaultHandler {
        private final BlockingQueue<Person> queue;

        private Person currentPerson;
        private StringBuilder text;

        PersonHandler(BlockingQueue<Person> queue) {
            this.queue = queue;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            text = new StringBuilder();

            if ("person".equals(qName)) {
                currentPerson = new Person();
                currentPerson.id = attrs.getValue("id");
            } else if (currentPerson != null) {
                String ref = attrs.getValue("ref");
                switch (qName) {
                    case "spouse" -> currentPerson.spouseId = ref;
                    case "father" -> currentPerson.fatherId = ref;
                    case "mother" -> currentPerson.motherId = ref;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (text != null) {
                text.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String value = text != null ? text.toString().trim() : "";

            if (currentPerson != null) {
                switch (qName) {
                    case "first-name" -> currentPerson.firstName = value.isEmpty() ? null : value;
                    case "last-name" -> currentPerson.lastName = value.isEmpty() ? null : value;
                    case "gender" -> {
                        if (!value.isEmpty()) {
                            currentPerson.gender = switch (value.toLowerCase()) {
                                case "male", "m" -> "M";
                                case "female", "f" -> "F";
                                default -> null;
                            };
                        }
                    }
                    case "person" -> {
                        try {
                            queue.put(currentPerson);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        currentPerson = null;
                    }
                }
            }
        }
    }

    private static class Person {
        static final Person POISON = new Person();
        String id;
        String firstName;
        String lastName;
        String gender;
        String spouseId;
        String fatherId;
        String motherId;
    }
}