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

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import slash.navigation.nmn.bindingcruiser.Route;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static org.eclipse.persistence.jaxb.MarshallerProperties.JSON_INCLUDE_ROOT;
import static org.eclipse.persistence.jaxb.MarshallerProperties.MEDIA_TYPE;

class NavigonCruiserUtil {

    private static JAXBContext newContext() throws JAXBException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(JAXB_FORMATTED_OUTPUT, true);
        properties.put(MEDIA_TYPE, "application/json");
        properties.put(JSON_INCLUDE_ROOT, true);
        return JAXBContextFactory.createContext(new Class[]{Route.class}, properties);
    }

    private static Unmarshaller newUnmarshaller() throws JAXBException {
        return newContext().createUnmarshaller();
    }

    private static Marshaller newMarshaller() throws JAXBException {
        return newContext().createMarshaller();
    }

    public static Route unmarshal(InputStream in) throws JAXBException {
        Route result;
        try {
            result = (Route) newUnmarshaller().unmarshal(in);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal(Route route, OutputStream out) throws JAXBException {
        try {
            try {
                newMarshaller().marshal(route, out);
            } finally {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            throw new JAXBException("Error while marshalling: " + e, e);
        }
    }
}
