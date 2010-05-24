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

package slash.navigation.lmx;

import slash.navigation.jaxb.JaxbUtils;
import slash.navigation.lmx.binding.Lmx;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class LmxUtil {
    private static final JAXBContext CONTEXT = JaxbUtils.newContext(slash.navigation.lmx.binding.ObjectFactory.class);

    private static final String LMX_NAMESPACE_URI = "http://www.nokia.com/schemas/location/landmarks/1/0";

    private static Unmarshaller newUnmarshaller() {
        return JaxbUtils.newUnmarshaller(CONTEXT);
    }

    private static Marshaller newMarshaller() {
        return JaxbUtils.newMarshaller(CONTEXT,
                LMX_NAMESPACE_URI, "lm"
        );
    }


    public static Lmx unmarshal(InputStream in) throws JAXBException {
        Lmx result = null;
        try {
            result = (Lmx) newUnmarshaller().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error with " + result + ": " + e.getMessage(), e);
        }
        return result;
    }

    public static void marshal(Lmx lmx, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(new JAXBElement<Lmx>(new QName(LMX_NAMESPACE_URI, "lmx"), Lmx.class, lmx), out);
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