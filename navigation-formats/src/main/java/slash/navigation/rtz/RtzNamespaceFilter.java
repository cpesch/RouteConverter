/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.rtz;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLFilterImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Rewrites namespace URIs while parsing, so RTZ 1.1 documents can be read
 * with the RTZ 1.0 bindings.
 *
 * @author Christian Pesch
 */
class RtzNamespaceFilter extends XMLFilterImpl {
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
