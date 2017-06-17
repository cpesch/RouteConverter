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

package slash.navigation.viamichelin;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import slash.common.helpers.JAXBHelper;
import slash.navigation.viamichelin.binding.ObjectFactory;
import slash.navigation.viamichelin.binding.PoiList;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;
import static slash.common.helpers.JAXBHelper.newContext;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;

class ViaMichelinUtil {
    private static final String XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String VIAMICHELIN_NAMESPACE_URI = "http://www2.viamichelin.com/vmw2/dtd/export.dtd";

    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        Marshaller marshaller = JAXBHelper.newMarshaller(newContext(ObjectFactory.class));
        try {
            marshaller.setProperty(JAXB_FRAGMENT, true);
            marshaller.setProperty(JAXB_ENCODING, ISO_LATIN1_ENCODING);
        } catch (PropertyException e) {
            // intentionally left empty
        }
        return marshaller;
    }


    private static XMLReader createXMLReader() throws JAXBException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        XMLReader xmlReader;
        try {
            saxParser = parserFactory.newSAXParser();
            xmlReader = saxParser.getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new JAXBException("Parser configuration error: " + e, e);
        } catch (SAXException e) {
            throw new JAXBException("SAX error: " + e, e);
        }
        EntityResolver entityResolver = new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                if (systemId != null && systemId.contains("viamichelin") && systemId.endsWith("export.dtd"))
                    return new InputSource(new ByteArrayInputStream(XML_PREAMBLE.getBytes()));
                else
                    return null;
            }
        };
        xmlReader.setEntityResolver(entityResolver);
        return xmlReader;
    }

    public static PoiList unmarshal(Reader reader) throws JAXBException {
        PoiList result;
        try {
            result = (PoiList) newUnmarshaller().unmarshal(new SAXSource(createXMLReader(), new InputSource(reader)));
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }

    public static void marshal(PoiList poiList, OutputStream out) throws JAXBException {
        try {
            try {
                String header = XML_PREAMBLE + "\n" +
                        "<!DOCTYPE poi_list SYSTEM \"" + VIAMICHELIN_NAMESPACE_URI + "\">";
                out.write(header.getBytes());
                newMarshaller().marshal(new JAXBElement<>(new QName("poi_list"), PoiList.class, poiList), out);
            }
            finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }
}
