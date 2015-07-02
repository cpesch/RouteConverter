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

package slash.navigation.routes.remote.helpers;

import slash.common.helpers.JAXBHelper;
import slash.navigation.routes.remote.binding.CatalogType;
import slash.navigation.routes.remote.binding.ObjectFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import static slash.common.helpers.JAXBHelper.newContext;

public class RoutesUtil {
    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    private static Marshaller newMarshaller() {
        return JAXBHelper.newMarshaller(newContext(ObjectFactory.class));
    }

    public static CatalogType unmarshal(String string) throws JAXBException {
        return unmarshal(new StringReader(string));
    }

    public static CatalogType unmarshal(Reader reader) throws JAXBException {
        CatalogType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller().unmarshal(reader);
            result = (CatalogType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e);
        }
        return result;
    }

    public static CatalogType unmarshal(InputStream in) throws JAXBException {
        CatalogType result;
        try {
            JAXBElement element = (JAXBElement) newUnmarshaller().unmarshal(in);
            result = (CatalogType) element.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
        return result;
    }

    public static void marshal(CatalogType catalogType, Writer writer) throws JAXBException {
        newMarshaller().marshal(new ObjectFactory().createCatalog(catalogType), writer);
    }
}
