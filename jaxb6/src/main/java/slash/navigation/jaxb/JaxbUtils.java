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

package slash.navigation.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.util.Arrays.asList;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

/**
 * Provides JAXB helpers.
 *
 * @author Christian Pesch
 */

public class JaxbUtils {
    private static final Logger log = Logger.getLogger(JaxbUtils.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(JaxbUtils.class);
    private static final String JAXB_IMPL_NAMESPACE_PREFIX_MAPPER = "com.sun.xml.internal.bind.namespacePrefixMapper".intern();
    public static final String JAXB_IMPL_HEADER = "com.sun.xml.internal.bind.xmlHeaders".intern();

    private static Map<List<Class<?>>, JAXBContext> classesToContext = new HashMap<List<Class<?>>, JAXBContext>();
    private static boolean cacheContexts = false;

    public static void setCacheContexts(boolean cacheContexts) {
        JaxbUtils.cacheContexts = cacheContexts;
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

    public static Marshaller newMarshaller(JAXBContext context, String... uriToPrefix) {
        try {
            Marshaller result = context.createMarshaller();
            result.setProperty(JAXB_FORMATTED_OUTPUT, preferences.getBoolean("prettyPrintXml", true));
            try {
                result.setProperty(JAXB_IMPL_NAMESPACE_PREFIX_MAPPER, new NamespacePrefixMapperImpl(map(uriToPrefix)));
            } catch (Throwable t) {
                t.printStackTrace();
                log.severe("Could not set namespace prefix mapper: " + t.getMessage());
            }
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

    private static Map<String, String> map(String... keyValue) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < keyValue.length; i += 2) result.put(keyValue[i], keyValue[i + 1]);
        return result;
    }
}
