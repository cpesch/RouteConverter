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

package slash.navigation.klicktel;

import slash.navigation.jaxb.JaxbUtils;
import slash.navigation.klicktel.binding.KDRoute;
import slash.navigation.klicktel.binding.ObjectFactory;
import slash.navigation.base.XmlNavigationFormat;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.*;

class KlickTelUtil {
    private static final JAXBContext CONTEXT = JaxbUtils.newContext(ObjectFactory.class);

    private static final String KLICKTEL_NAMESPACE_URI = "";

    private static Unmarshaller newUnmarshaller() {
        return JaxbUtils.newUnmarshaller(CONTEXT);
    }

    private static Marshaller newMarshaller() {
        Marshaller marshaller = JaxbUtils.newMarshaller(CONTEXT);
        try {
            marshaller.setProperty(JaxbUtils.JAXB_IMPL_HEADER, XmlNavigationFormat.HEADER);
        } catch (PropertyException e) {
            // intentionally left empty
        }
        return marshaller;
    }


    public static KDRoute unmarshal(InputStream in) throws JAXBException {
        KDRoute result = null;
        try {
            result = (KDRoute) newUnmarshaller().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        }
        return result;
    }


    public static void marshal(KDRoute tour, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new JAXBElement<KDRoute>(new QName(KLICKTEL_NAMESPACE_URI, "kDRoute"), KDRoute.class, tour), out);
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
