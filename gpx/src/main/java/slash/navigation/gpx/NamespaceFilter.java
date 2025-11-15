package slash.navigation.gpx;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLFilterImpl;

import java.util.HashMap;
import java.util.Map;

class NamespaceFilter extends XMLFilterImpl {
    private final Map<String, String> uriMap = new HashMap<>();

    public void addMapping(String fromUri, String toUri) {
        uriMap.put(fromUri, toUri);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(mapUri(uri), localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(mapUri(uri), localName, qName);
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        super.startPrefixMapping(prefix, mapUri(uri));
    }

    private String mapUri(String uri) {
        return uriMap.getOrDefault(uri, uri);
    }
}
