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
import slash.navigation.jaxb.JaxbUtils;
import slash.navigation.viamichelin.binding.ObjectFactory;
import slash.navigation.viamichelin.binding.PoiList;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.*;

public class ViaMichelinUtil {
    public static final JAXBContext CONTEXT = JaxbUtils.newContext(ObjectFactory.class);

    public static final String VIAMICHELIN_NAMESPACE_URI = "http://www2.viamichelin.com/vmw2/dtd/export.dtd";

    public static Unmarshaller newUnmarshaller() {
        return JaxbUtils.newUnmarshaller(CONTEXT);
    }

    public static Marshaller newMarshaller() {
        Marshaller marshaller = JaxbUtils.newMarshaller(CONTEXT);
        try {
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-1");
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
            throw new JAXBException("Parser configuration error: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new JAXBException("SAX error: " + e.getMessage(), e);
        }
        EntityResolver entityResolver = new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                if (systemId != null && systemId.contains("viamichelin") && systemId.endsWith("export.dtd"))
                    return new InputSource(new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
                else
                    return null;
            }
        };
        xmlReader.setEntityResolver(entityResolver);
        return xmlReader;
    }

    public static PoiList unmarshal(Reader reader) throws JAXBException {
        PoiList result = null;
        try {
            result = (PoiList) newUnmarshaller().unmarshal(new SAXSource(createXMLReader(), new InputSource(reader)));
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage());
        }
        return result;
    }

    public static PoiList unmarshal(InputStream in) throws JAXBException {
        PoiList result = null;
        try {
            result = (PoiList) newUnmarshaller().unmarshal(new SAXSource(createXMLReader(), new InputSource(in)));
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        }
        return result;
    }

    public static PoiList unmarshal(File file) throws JAXBException {
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                return unmarshal(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while unmarshalling from " + file + ": " + e.getMessage());
        }
    }


    public static void marshal(PoiList poiList, File file) throws JAXBException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<!DOCTYPE poi_list SYSTEM \"" + VIAMICHELIN_NAMESPACE_URI + "\">";
                fos.write(header.getBytes());
                newMarshaller().marshal(new JAXBElement<PoiList>(new QName("poi_list"), PoiList.class, poiList), fos);
            }
            finally {
                fos.flush();
                fos.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling to " + file + ": " + e.getMessage());
        }
    }
}
