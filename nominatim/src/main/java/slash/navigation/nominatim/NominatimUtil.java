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

package slash.navigation.nominatim;

import slash.common.helpers.JAXBHelper;
import slash.navigation.nominatim.reverse.ReversegeocodeType;
import slash.navigation.nominatim.search.SearchresultsType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

import static slash.common.helpers.JAXBHelper.newContext;

class NominatimUtil {
    private static Unmarshaller newUnmarshallerSearch() {
        return JAXBHelper.newUnmarshaller(newContext(slash.navigation.nominatim.search.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshallerReverse() {
        return JAXBHelper.newUnmarshaller(newContext(slash.navigation.nominatim.reverse.ObjectFactory.class));
    }

    public static SearchresultsType unmarshalSearch(String string) throws JAXBException {
        try (StringReader reader = new StringReader(string)){
            @SuppressWarnings("unchecked")
            JAXBElement<SearchresultsType> unmarshal = (JAXBElement<SearchresultsType>) newUnmarshallerSearch().unmarshal(reader);
            return unmarshal.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
    }

    public static ReversegeocodeType unmarshalReverse(String string) throws JAXBException {
        try (StringReader reader = new StringReader(string)){
            @SuppressWarnings("unchecked")
            JAXBElement<ReversegeocodeType> unmarshal = (JAXBElement<ReversegeocodeType>) newUnmarshallerReverse().unmarshal(reader);
            return unmarshal.getValue();
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        }
    }
}