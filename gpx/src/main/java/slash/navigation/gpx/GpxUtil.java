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

package slash.navigation.gpx;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding11.ExtensionsType;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.MetadataType;
import slash.navigation.gpx.binding11.RteType;
import slash.navigation.gpx.binding11.TrkType;
import slash.navigation.gpx.binding11.TrksegType;
import slash.navigation.gpx.binding11.WptType;

import jakarta.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

import static slash.common.helpers.JAXBHelper.*;

public class GpxUtil {
    public static final String GPX_10_NAMESPACE_URI = "http://www.topografix.com/GPX/1/0";
    public static final String GPX_11_NAMESPACE_URI = "http://www.topografix.com/GPX/1/1";
    public static final String GARMIN_EXTENSIONS_3_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/GpxExtensions/v3";
    public static final String GARMIN_TRACKPOINT_EXTENSIONS_1_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";
    public static final String GARMIN_TRACKPOINT_EXTENSIONS_2_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/TrackPointExtension/v2";
    public static final String GARMIN_TRIP_EXTENSIONS_1_NAMESPACE_URI = "http://www.garmin.com/xmlschemas/TripExtensions/v1";
    public static final String OSMAND_EXTENSIONS_NAMESPACE_URI = "https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx";
    public static final String TREKBUDDY_EXTENSIONS_0984_NAMESPACE_URI = "http://trekbuddy.net/2009/01/gpx/nmea";

    public static Unmarshaller newUnmarshaller10() {
        return newUnmarshaller(newContext(slash.navigation.gpx.binding10.ObjectFactory.class));
    }

    private static Marshaller newMarshaller10() {
        return newMarshaller(newContext(slash.navigation.gpx.binding10.ObjectFactory.class));
    }

    private static JAXBContext newContext11() {
        return newContext(slash.navigation.gpx.binding11.ObjectFactory.class,
                slash.navigation.gpx.garmin3.ObjectFactory.class,
                slash.navigation.gpx.osmand.ObjectFactory.class,
                slash.navigation.gpx.trackpoint1.ObjectFactory.class,
                slash.navigation.gpx.trackpoint2.ObjectFactory.class,
                slash.navigation.gpx.trip1.ObjectFactory.class,
                slash.navigation.gpx.trekbuddy.ObjectFactory.class);
    }

    private static Unmarshaller newUnmarshaller11() {
        return newUnmarshaller(newContext11());
    }

    private static Marshaller newMarshaller11() {
        return newMarshaller(newContext11());
    }


    public static Gpx unmarshal10(Reader reader) throws IOException {
        Gpx result;
        try {
            result = (Gpx) newUnmarshaller10().unmarshal(reader);
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static Gpx unmarshal10(InputStream inputStream) throws IOException {
        Gpx result;
        try {
            result = (Gpx) newUnmarshaller10().unmarshal(inputStream);
        } catch (ClassCastException | JAXBException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal10(Gpx gpx, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller10().marshal(new JAXBElement<>(new QName(GPX_10_NAMESPACE_URI, "gpx"), Gpx.class, gpx), outputStream);
            }
            finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }


    public static GpxType unmarshal11(String string) throws IOException {
        return unmarshal11(new StringReader(string));
    }

    private static GpxType unmarshal11Internal(InputSource inputSource) throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            NamespaceFilter filter = new NamespaceFilter();
            filter.addMapping("https://www8.garmin.com/xmlschemas/TrackPointExtensionv1.xsd",
                    "http://www.garmin.com/xmlschemas/TrackPointExtension/v1");
            filter.setParent(saxParser.getXMLReader());

            SAXSource source = new SAXSource(filter, inputSource);
            JAXBElement<?> element = (JAXBElement<?>) newUnmarshaller11().unmarshal(source);
            return (GpxType) element.getValue();
        } catch (ClassCastException | JAXBException | SAXException | ParserConfigurationException e) {
            throw new IOException("Parse error: " + e, e);
        }
    }

    public static GpxType unmarshal11(Reader reader) throws IOException {
        return unmarshal11Internal(new InputSource(reader));
    }

    public static GpxType unmarshal11(InputStream in) throws IOException {
        return unmarshal11Internal(new InputSource(in));
    }

    /**
     * The JAXB unmarshaller materializes extension elements it has no binding for as DOM elements and
     * copies all namespace declarations that were in scope in the parsed document onto them. Marshalling
     * writes those declarations out verbatim, so a bare {@code <speed>} read from a document whose root
     * declared {@code xmlns:cb} is written as {@code <speed xmlns:cb="...">}. Drop the declarations that
     * the element and its descendants don't use.
     */
    private static void removeUnusedNamespaceDeclarations(Element element) {
        Set<String> usedNamespaceUris = new HashSet<>();
        collectUsedNamespaceUris(element, usedNamespaceUris);

        List<Attr> unused = new ArrayList<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            // only prefixed declarations, the default namespace decides the element's own name
            if (XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI()) && attribute.getPrefix() != null &&
                    !usedNamespaceUris.contains(attribute.getValue()))
                unused.add(attribute);
        }
        for (Attr attribute : unused)
            element.removeAttributeNode(attribute);
    }

    private static void collectUsedNamespaceUris(Node node, Set<String> namespaceUris) {
        if (node.getNamespaceURI() != null)
            namespaceUris.add(node.getNamespaceURI());

        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null)
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if (!XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI()) && attribute.getNamespaceURI() != null)
                    namespaceUris.add(attribute.getNamespaceURI());
            }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
            collectUsedNamespaceUris(children.item(i), namespaceUris);
    }

    private static void removeUnusedNamespaceDeclarations(ExtensionsType extensions) {
        if (extensions == null)
            return;
        for (Object any : extensions.getAny())
            if (any instanceof Element)
                removeUnusedNamespaceDeclarations((Element) any);
    }

    private static void removeUnusedNamespaceDeclarations(GpxType gpxType) {
        removeUnusedNamespaceDeclarations(gpxType.getExtensions());

        MetadataType metadata = gpxType.getMetadata();
        if (metadata != null)
            removeUnusedNamespaceDeclarations(metadata.getExtensions());

        for (WptType wpt : gpxType.getWpt())
            removeUnusedNamespaceDeclarations(wpt.getExtensions());

        for (RteType rte : gpxType.getRte()) {
            removeUnusedNamespaceDeclarations(rte.getExtensions());
            for (WptType rtept : rte.getRtept())
                removeUnusedNamespaceDeclarations(rtept.getExtensions());
        }

        for (TrkType trk : gpxType.getTrk()) {
            removeUnusedNamespaceDeclarations(trk.getExtensions());
            for (TrksegType trkseg : trk.getTrkseg()) {
                removeUnusedNamespaceDeclarations(trkseg.getExtensions());
                for (WptType trkpt : trkseg.getTrkpt())
                    removeUnusedNamespaceDeclarations(trkpt.getExtensions());
            }
        }
    }

    public static void marshal11(GpxType gpxType, Writer writer) throws JAXBException {
        removeUnusedNamespaceDeclarations(gpxType);
        newMarshaller11().marshal(new slash.navigation.gpx.binding11.ObjectFactory().createGpx(gpxType), writer);
    }

    public static void marshal11(GpxType gpxType, OutputStream outputStream) throws JAXBException {
        removeUnusedNamespaceDeclarations(gpxType);
        try {
            try {
                newMarshaller11().marshal(new slash.navigation.gpx.binding11.ObjectFactory().createGpx(gpxType), outputStream);
            }
            finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }

    public static String toXml(GpxType gpxType) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            marshal11(gpxType, writer);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + gpxType + ": " + e, e);
        }
        return writer.toString();
    }
}
