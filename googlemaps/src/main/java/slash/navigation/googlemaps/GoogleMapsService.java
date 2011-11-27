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

import slash.common.util.Bearing;
import slash.navigation.googlemaps.geocode.GeocodeResponse;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;

import static slash.navigation.rest.Helper.encodeUri;

/**
 * Encapsulates REST access to the Google Maps API Geocoding Service.
 *
 * @author Christian Pesch
 */

public class GoogleMapsService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleMapsService.class);
    private static final String GOOGLE_MAPS_API_URL_PREFERENCE = "googleMapsApiUrl";

    private static String getGoogleMapsApiUrl(String api, String payload) {
        String language = Locale.getDefault().getLanguage();
        return preferences.get(GOOGLE_MAPS_API_URL_PREFERENCE, "http://maps.googleapis.com/") +
                "maps/api/" + api + "/xml?" + payload + "&sensor=false&language=" + language;
    }

    private static String getElevationUrl(String payload) {
        return getGoogleMapsApiUrl("elevation", payload);
    }

    private static String getGeocodingUrl(String payload) {
        return getGoogleMapsApiUrl("geocode", payload);
    }

    public String getLocationFor(double longitude, double latitude) throws IOException {
        String url = getGeocodingUrl("latlng=" + latitude + "," + longitude);
        Get get = new Get(url);
        String result = get.execute();
        if (get.isSuccessful())
            try {
                GeocodeResponse geocodeResponse = GoogleMapsUtil.unmarshalGeocode(result);
                if (geocodeResponse != null) {
                    String status = geocodeResponse.getStatus();
                    if (status.equals("OK"))
                        return extractClosestLocation(geocodeResponse.getResult(), longitude, latitude);
                    if (status.equals("OVER_QUERY_LIMIT"))
                        throw new ServiceUnavailableException("maps.googleapis.com", url);
                }
            } catch (JAXBException e) {
                IOException io = new IOException("Cannot unmarshall " + result + ": " + e.getMessage());
                io.setStackTrace(e.getStackTrace());
                throw io;
            }
        return null;
    }

    private String extractClosestLocation(List<GeocodeResponse.Result> results,
                                          final double longitude, final double latitude) {
        GeocodeResponse.Result[] resultsArray = results.toArray(new GeocodeResponse.Result[results.size()]);
        Arrays.sort(resultsArray, new Comparator<GeocodeResponse.Result>() {
            public int compare(GeocodeResponse.Result p1, GeocodeResponse.Result p2) {
                GeocodeResponse.Result.Geometry.Location l1 = p1.getGeometry().getLocation();
                GeocodeResponse.Result.Geometry.Location l2 = p2.getGeometry().getLocation();
                double distance1 = Bearing.calculateBearing(longitude, latitude, l1.getLng().doubleValue(), l1.getLat().doubleValue()).getDistance();
                double distance2 = Bearing.calculateBearing(longitude, latitude, l2.getLng().doubleValue(), l2.getLat().doubleValue()).getDistance();
                return new Double(distance1 - distance2).intValue();
            }
        });
        return resultsArray[0].getFormattedAddress();
    }

    public GoogleMapsPosition getPositionFor(String address) throws IOException {
        List<GoogleMapsPosition> positions = getPositionsFor(address);
        return positions != null && positions.size() > 0 ? positions.get(0) : null;
    }

    public List<GoogleMapsPosition> getPositionsFor(String address) throws IOException {
        String url = getGeocodingUrl("address=" + encodeUri(address));
        Get get = new Get(url);
        String result = get.execute();
        if (get.isSuccessful())
            try {
                GeocodeResponse geocodeResponse = GoogleMapsUtil.unmarshalGeocode(result);
                if (geocodeResponse != null) {
                    String status = geocodeResponse.getStatus();
                    if (status.equals("OK"))
                        return extractAdresses(geocodeResponse.getResult());
                    if (status.equals("OVER_QUERY_LIMIT"))
                        throw new ServiceUnavailableException("maps.googleapis.com", url);
                }
            } catch (JAXBException e) {
                IOException io = new IOException("Cannot unmarshall " + result + ": " + e.getMessage());
                io.setStackTrace(e.getStackTrace());
                throw io;
            }
        return null;
    }

    private List<GoogleMapsPosition> extractAdresses(List<GeocodeResponse.Result> responses) {
        List<GoogleMapsPosition> result = new ArrayList<GoogleMapsPosition>(responses.size());
        for (GeocodeResponse.Result response : responses) {
            GeocodeResponse.Result.Geometry.Location location = response.getGeometry().getLocation();
            result.add(new GoogleMapsPosition(location.getLng().doubleValue(), location.getLat().doubleValue(),
                    0.0d, response.getFormattedAddress()));
        }
        return result;
    }
}
