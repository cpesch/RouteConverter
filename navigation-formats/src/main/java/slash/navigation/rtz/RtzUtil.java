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

import slash.common.helpers.JAXBHelper;
import slash.navigation.rtz.binding10.ObjectFactory;
import slash.navigation.rtz.binding10.Route;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static jakarta.xml.bind.Marshaller.JAXB_ENCODING;
import static slash.common.helpers.JAXBHelper.newContext;
import static slash.common.io.Transfer.UTF8_ENCODING;

class RtzUtil {
    static final String RTZ_10_NAMESPACE_URI = "http://www.cirm.org/RTZ/1/0";
    static final String RTZ_11_NAMESPACE_URI = "http://www.cirm.org/RTZ/1/1";

    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        Marshaller marshaller = JAXBHelper.newMarshaller(newContext(ObjectFactory.class));
        try {
            marshaller.setProperty(JAXB_ENCODING, UTF8_ENCODING);
        } catch (PropertyException e) {
            // intentionally left empty
        }
        return marshaller;
    }

    public static Route unmarshal(InputStream in) throws IOException {
        Route result;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            // RTZ 1.1 is structurally identical to 1.0, so read it with the 1.0 bindings
            RtzNamespaceFilter filter = new RtzNamespaceFilter();
            filter.addMapping(RTZ_11_NAMESPACE_URI, RTZ_10_NAMESPACE_URI);
            filter.setParent(saxParser.getXMLReader());

            Object unmarshalled = newUnmarshaller().unmarshal(new SAXSource(filter, new InputSource(in)));
            result = unmarshalled instanceof JAXBElement<?> element ? (Route) element.getValue() : (Route) unmarshalled;
        } catch (ClassCastException | JAXBException | SAXException | ParserConfigurationException e) {
            throw new IOException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal(Route route, OutputStream outputStream) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new JAXBElement<>(new QName(RTZ_10_NAMESPACE_URI, "route"), Route.class, route), outputStream);
            }
            finally {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }
}
