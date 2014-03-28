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

package slash.navigation.nmn;

import slash.navigation.jaxb.JaxbUtils;
import slash.navigation.nmn.binding7.ObjectFactory;
import slash.navigation.nmn.binding7.Route;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;
import static slash.navigation.jaxb.JaxbUtils.newContext;

class Nmn7Util {
    private static final String NMN7_NAMESPACE_URI = "";

    private static Unmarshaller newUnmarshaller() {
        return JaxbUtils.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        Marshaller marshaller = JaxbUtils.newMarshaller(newContext(ObjectFactory.class));
        try {
            marshaller.setProperty(JAXB_ENCODING, ISO_LATIN1_ENCODING);
        } catch (PropertyException e) {
            // intentionally left empty
        }
        return marshaller;
    }

    public static Route unmarshal(InputStream in) throws JAXBException {
        Route result = null;
        try {
            result = (Route) newUnmarshaller().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        }
        return result;
    }

    public static void marshal(Route route, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new JAXBElement<Route>(new QName(NMN7_NAMESPACE_URI, "Route"), Route.class, route), out);
            }
            finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e.getMessage());
        }
    }
}