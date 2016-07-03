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

package slash.navigation.geonames;

import slash.common.helpers.JAXBHelper;
import slash.navigation.geonames.binding.Geonames;
import slash.navigation.geonames.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

import static slash.common.helpers.JAXBHelper.newContext;

class GeoNamesUtil {
    private static Unmarshaller newUnmarshaller() {
        return JAXBHelper.newUnmarshaller(newContext(ObjectFactory.class));
    }

    public static Geonames unmarshal(String string) throws JAXBException {
        try (StringReader reader = new StringReader(string)){
            return (Geonames) newUnmarshaller().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
    }
}