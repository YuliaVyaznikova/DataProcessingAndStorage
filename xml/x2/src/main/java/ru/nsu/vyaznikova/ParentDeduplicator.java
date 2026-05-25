package ru.nsu.vyaznikova;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class ParentDeduplicator {

    public void deduplicate(Path input, Path output) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();

        SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
        TransformerHandler th = stf.newTransformerHandler();
        th.setResult(new StreamResult(output.toFile()));

        ParentFilter filter = new ParentFilter(parser.getXMLReader());
        filter.setContentHandler(th);

        try (FileInputStream fis = new FileInputStream(input.toFile())) {
            filter.parse(new InputSource(fis));
        }
    }

    private static class ParentFilter extends XMLFilterImpl {
        private int parentsDepth = 0;
        private boolean seenFather = false;
        private boolean seenMother = false;
        private boolean skipping = false;

        ParentFilter(org.xml.sax.XMLReader parent) {
            super(parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 org.xml.sax.Attributes atts) throws SAXException {
            if ("parents".equals(localName)) {
                parentsDepth++;
                seenFather = false;
                seenMother = false;
            }

            if (parentsDepth > 0) {
                if ("father".equals(localName)) {
                    if (seenFather) { skipping = true; return; }
                    seenFather = true;
                } else if ("mother".equals(localName)) {
                    if (seenMother) { skipping = true; return; }
                    seenMother = true;
                }
            }

            super.startElement(uri, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (skipping) { skipping = false; return; }
            super.endElement(uri, localName, qName);
            if ("parents".equals(localName)) {
                parentsDepth--;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (!skipping) super.characters(ch, start, length);
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (!skipping) super.ignorableWhitespace(ch, start, length);
        }
    }
}