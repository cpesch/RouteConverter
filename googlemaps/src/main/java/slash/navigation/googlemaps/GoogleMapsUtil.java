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

package slash.navigation.googlemaps;

import slash.common.helpers.JAXBHelper;
import slash.navigation.googlemaps.elevation.ElevationResponse;
import slash.navigation.googlemaps.geocode.GeocodeResponse;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

import static slash.common.helpers.JAXBHelper.newContext;

public class GoogleMapsUtil {
    private static Unmarshaller newUnmarshallerElevation() {
        return JAXBHelper.newUnmarshaller(newContext(slash.navigation.googlemaps.elevation.ObjectFactory.class));
    }

    private static Unmarshaller newUnmarshallerGeocode() {
        return JAXBHelper.newUnmarshaller(newContext(slash.navigation.googlemaps.geocode.ObjectFactory.class));
    }

    private static ElevationResponse unmarshalElevation(StringReader reader) throws JAXBException {
        ElevationResponse result = null;
        try {
            result = (ElevationResponse) newUnmarshallerElevation().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        } finally {
            reader.close();
        }
        return result;
    }

    public static ElevationResponse unmarshalElevation(String string) throws JAXBException {
        return unmarshalElevation(new StringReader(string));
    }

    private static GeocodeResponse unmarshalGeocode(StringReader reader) throws JAXBException {
        GeocodeResponse result = null;
        try {
            result = (GeocodeResponse) newUnmarshallerGeocode().unmarshal(reader);
        } catch (ClassCastException e) {
            throw new JAXBException("Parse error: " + e, e);
        } finally {
            reader.close();
        }
        return result;
    }

    public static GeocodeResponse unmarshalGeocode(String string) throws JAXBException {
        return unmarshalGeocode(new StringReader(string));
    }
}
