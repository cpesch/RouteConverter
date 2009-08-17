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

import slash.navigation.kml.KmlUtil;
import slash.navigation.kml.binding20.*;
import slash.navigation.rest.Get;
import slash.navigation.util.Conversion;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Encapsulates REST access to the Google Maps API Geocoding Service.
 *
 * @author Christian Pesch
 */

public class GoogleMapsService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleMapsService.class);
    private static final String GOOGLE_MAPS_URL_PREFERENCE = "googleMapsUrl";

    private static String getGoogleMapsUrlPreference() {
        return preferences.get(GOOGLE_MAPS_URL_PREFERENCE, "http://maps.google.com/");
    }

    public String getLocationFor(double longitude, double latitude) throws IOException {
        Get get = new Get(getGoogleMapsUrlPreference() + "maps/geo?q=" + latitude + "," + longitude +
                "&output=kml&oe=utf8&sensor=false&key=ABQIAAAA3C3cggohQH044oJU10p9hRSfCfkamzr65RA-A3ZfXmc8dgIhVxTusI-8RzngggpTq0xoW5B1StZwug");
        String result = get.execute();
        if (get.isSuccessful())
            try {
                Kml kml = KmlUtil.unmarshal20(result);
                if (kml != null) {
                    if (extractStatusCode(kml) == 200) {
                        return extractHighestAccuracyLocation(kml);
                    }
                }
            } catch (JAXBException e) {
                IOException io = new IOException("Cannot unmarshall " + result + ": " + e.getMessage());
                io.setStackTrace(e.getStackTrace());
                throw io;
            }
        return null;
    }

    <T> T find(List elements, Class<T> resultClass) {
        for (Object element : elements) {
            if (resultClass.isInstance(element))
                return (T) element;
            if (element instanceof JAXBElement) {
                JAXBElement jaxbElement = (JAXBElement) element;
                if (resultClass.isInstance(jaxbElement.getValue()))
                    return (T) jaxbElement.getValue();
            }
        }
        return null;
    }

    int extractStatusCode(Kml kml) {
        Response response = kml.getResponse();
        if (response != null) {
            Status status = find(response.getNameOrStatusOrPlacemark(), Status.class);
            if (status != null) {
                Integer code = find(status.getCodeOrRequest(), Integer.class);
                if (code != null)
                    return code;
            }
        }
        return -1;
    }

    <T> List<T> findAll(List elements, Class<T> resultClass) {
        List<T> result = new ArrayList<T>();
        for (Object element : elements) {
            if (resultClass.isInstance(element))
                result.add((T) element);
        }
        return result.size() > 0 ? result : null;
    }

    List<Placemark> extractPlacemarks(Kml kml) {
        Response response = kml.getResponse();
        if (response != null) {
            List<Placemark> placemarks = findAll(response.getNameOrStatusOrPlacemark(), Placemark.class);
            if (placemarks != null) {
                return placemarks;
            }
        }
        return null;
    }

    Placemark extractHighestAccuracyPlacemark(Kml kml) {
        List<Placemark> placemarks = extractPlacemarks(kml);
        if (placemarks != null && placemarks.size() > 0) {
            Placemark[] placemarksArray = placemarks.toArray(new Placemark[placemarks.size()]);
            Arrays.sort(placemarksArray, new Comparator<Placemark>() {
                public int compare(Placemark p1, Placemark p2) {
                    AddressDetails a1 = find(p1.getDescriptionOrNameOrSnippet(), AddressDetails.class);
                    AddressDetails a2 = find(p2.getDescriptionOrNameOrSnippet(), AddressDetails.class);
                    Integer accuracy1 = Conversion.parseInt(a1.getOtherAttributes().get(new QName("Accuracy")));
                    Integer accuracy2 = Conversion.parseInt(a2.getOtherAttributes().get(new QName("Accuracy")));
                    return accuracy2.compareTo(accuracy1);
                }
            });
            return placemarksArray[0];
        }
        return null;
    }

    String extractHighestAccuracyLocation(Kml kml) {
        Placemark placemark = extractHighestAccuracyPlacemark(kml);
        if (placemark != null) {
            return find(placemark.getDescriptionOrNameOrSnippet(), String.class);
        }
        return null;
    }
}
