package ru.nsu.vyaznikova;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class StreamingLoader {
    private final DatabaseManager db;
    private final int threads;

    public StreamingLoader(DatabaseManager db, int threads) {
        this.db = db;
        this.threads = threads;
    }

    public void load(String xmlFile) throws Exception {
        db.createTempSchema();

        File file = new File(xmlFile);
        long fileSize = file.length();
        
        List<Segment> segments = splitFileIntoSegments(file, fileSize, threads);
        System.out.println("Split into " + segments.size() + " segments");
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        
        for (Segment segment : segments) {
            futures.add(executor.submit(() -> {
                try {
                    parseSegment(file, segment);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        
        for (Future<?> f : futures) {
            f.get();
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        
        System.out.println("Streaming phase completed");
    }
    
    private List<Segment> splitFileIntoSegments(File file, long fileSize, int n) throws IOException {
        List<Segment> segments = new ArrayList<>();
        
        if (n <= 1) {
            segments.add(new Segment(0, fileSize));
            return segments;
        }
        
        long segmentSize = fileSize / n;
        long[] boundaries = new long[n + 1];
        boundaries[0] = 0;
        boundaries[n] = fileSize;
        
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (int i = 1; i < n; i++) {
                long approx = i * segmentSize;
                long boundary = findPersonBoundary(raf, approx);
                boundaries[i] = boundary;
            }
        }
        
        for (int i = 0; i < n; i++) {
            segments.add(new Segment(boundaries[i], boundaries[i + 1]));
        }
        
        return segments;
    }
    
    private long findPersonBoundary(RandomAccessFile raf, long approx) throws IOException {
        raf.seek(approx);
        
        byte[] buffer = new byte[8192];
        int read = raf.read(buffer);
        if (read <= 0) return approx;
        
        String chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
        
        int personStart = chunk.indexOf("<person");
        if (personStart >= 0) {
            return approx + personStart;
        }
        
        int personEnd = chunk.indexOf("</person>");
        if (personEnd >= 0) {
            return approx + personEnd + 9;
        }
        
        return approx;
    }
    
    private void parseSegment(File file, Segment segment) throws Exception {
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
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             SegmentInputStream sis = new SegmentInputStream(file, segment)) {
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            SegmentHandler handler = new SegmentHandler(ps, conn);
            
            parser.parse(sis, handler);
        }
    }
    
    private static class Segment {
        final long start;
        final long end;
        
        Segment(long start, long end) {
            this.start = start;
            this.end = end;
        }
        
        long size() {
            return end - start;
        }
    }
    
    private static class SegmentInputStream extends InputStream {
        private final RandomAccessFile raf;
        private final long end;
        private long position;
        private boolean headerSent = false;
        private boolean footerSent = false;
        private byte[] header = "<people>".getBytes(StandardCharsets.UTF_8);
        private byte[] footer = "</people>".getBytes(StandardCharsets.UTF_8);
        private int headerPos = 0;
        private int footerPos = 0;
        private boolean skipXmlDecl;
        private byte[] buffer = new byte[8192];
        private int bufPos = 0;
        private int bufLen = 0;
        private boolean inSkipMode = false;
        private byte[] skipPattern;
        private int skipPos = 0;
        
        SegmentInputStream(File file, Segment segment) throws IOException {
            this.raf = new RandomAccessFile(file, "r");
            this.raf.seek(segment.start);
            this.position = segment.start;
            this.end = segment.end;
            this.skipXmlDecl = (segment.start == 0);
        }
        
        private int readRaw() throws IOException {
            if (position >= end) return -1;
            int b = raf.read();
            if (b >= 0) position++;
            return b;
        }
        
        private boolean skipTag(String tag) throws IOException {
            byte[] tagBytes = tag.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < tagBytes.length; i++) {
                int b = readRaw();
                if (b < 0 || b != (tagBytes[i] & 0xFF)) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public int read() throws IOException {
            if (skipXmlDecl) {
                skipXmlDecl = false;
                if (skipTag("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")) {
                    return read();
                }
            }
            
            if (!headerSent) {
                if (headerPos < header.length) {
                    return header[headerPos++] & 0xFF;
                }
                headerSent = true;
            }
            
            if (position < end) {
                int b = readRaw();
                if (b == '<') {
                    long savedPos = position;
                    int firstChar = readRaw();
                    if (firstChar == 'p') {
                        if (skipTag("eople>") || skipTag("eople/>")) {
                            return read();
                        }
                    } else if (firstChar == '/') {
                        if (skipTag("people>")) {
                            return read();
                        }
                    }
                    raf.seek(savedPos);
                    position = savedPos;
                }
                return b;
            }
            
            if (!footerSent) {
                if (footerPos < footer.length) {
                    return footer[footerPos++] & 0xFF;
                }
                footerSent = true;
            }
            
            return -1;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int totalRead = 0;
            
            while (totalRead < len) {
                int next = read();
                if (next < 0) {
                    return totalRead > 0 ? totalRead : -1;
                }
                b[off + totalRead] = (byte) next;
                totalRead++;
            }
            
            return totalRead;
        }
        
        @Override
        public void close() throws IOException {
            raf.close();
        }
    }
    
    private static class SegmentHandler extends DefaultHandler {
        private final PreparedStatement ps;
        private final Connection conn;
        private Person currentPerson;
        private StringBuilder text;
        private int count = 0;
        
        SegmentHandler(PreparedStatement ps, Connection conn) {
            this.ps = ps;
            this.conn = conn;
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
                            ps.setString(1, currentPerson.id);
                            ps.setString(2, currentPerson.firstName);
                            ps.setString(3, currentPerson.lastName);
                            ps.setString(4, currentPerson.gender);
                            ps.setString(5, currentPerson.spouseId);
                            ps.setString(6, currentPerson.fatherId);
                            ps.setString(7, currentPerson.motherId);
                            ps.executeUpdate();
                            count++;
                            if (count % 1000 == 0) {
                                conn.commit();
                            }
                        } catch (SQLException e) {
                            throw new SAXException(e);
                        }
                        currentPerson = null;
                    }
                }
            }
        }
        
        @Override
        public void endDocument() throws SAXException {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new SAXException(e);
            }
        }
    }
    
    private static class Person {
        String id;
        String firstName;
        String lastName;
        String gender;
        String spouseId;
        String fatherId;
        String motherId;
    }
}