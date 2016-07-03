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

package slash.common.helpers;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import static java.util.Arrays.asList;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

/**
 * Provides JAXB helpers.
 *
 * @author Christian Pesch
 */

public class JAXBHelper {
    private static final Preferences preferences = Preferences.userNodeForPackage(JAXBHelper.class);
    public static final String JAXB_IMPL_HEADER = "com.sun.xml.internal.bind.xmlHeaders";

    private static Map<List<Class<?>>, JAXBContext> classesToContext = new HashMap<>();
    private static boolean cacheContexts = false;

    public static void setCacheContexts(boolean cacheContexts) {
        JAXBHelper.cacheContexts = cacheContexts;
    }

    public static JAXBContext newContext(Class<?>... classes) {
        List<Class<?>> key = asList(classes);
        JAXBContext context = classesToContext.get(key);
        if (context == null) {
            try {
                context = JAXBContext.newInstance(classes);
                if (cacheContexts)
                    classesToContext.put(key, context);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return context;
    }

    public static Marshaller newMarshaller(JAXBContext context) {
        try {
            Marshaller result = context.createMarshaller();
            result.setProperty(JAXB_FORMATTED_OUTPUT, preferences.getBoolean("prettyPrintXml", true));
            return result;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Unmarshaller newUnmarshaller(JAXBContext context) {
        try {
            return context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
