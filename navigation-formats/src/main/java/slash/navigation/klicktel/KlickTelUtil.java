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

import slash.common.helpers.JAXBHelper;
import slash.navigation.klicktel.binding.KDRoute;
import slash.navigation.klicktel.binding.ObjectFactory;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static slash.common.helpers.JAXBHelper.JAXB_IMPL_HEADER;
import static slash.common.helpers.JAXBHelper.newContext;
import static slash.navigation.base.XmlNavigationFormat.HEADER_LINE;

class KlickTelUtil {
    private static final String KLICKTEL_NAMESPACE_URI = "";

    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        Marshaller marshaller = JAXBHelper.newMarshaller(newContext(ObjectFactory.class));
        try {
            marshaller.setProperty(JAXB_IMPL_HEADER, HEADER_LINE);
        } catch (PropertyException e) {
            // intentionally left empty
        }
        return marshaller;
    }


    public static KDRoute unmarshal(InputStream in) throws JAXBException {
        KDRoute result;
        try {
            result = (KDRoute) newUnmarshaller().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }


    public static void marshal(KDRoute route, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new JAXBElement<>(new QName(KLICKTEL_NAMESPACE_URI, "kDRoute"), KDRoute.class, route), out);
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
