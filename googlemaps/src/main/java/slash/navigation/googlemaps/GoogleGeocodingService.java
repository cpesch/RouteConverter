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

import jakarta.xml.bind.JAXBException;
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;
import slash.navigation.googlemaps.geocode.GeocodeResponse;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static slash.common.io.Transfer.encodeUri;
import static slash.navigation.common.Bearing.calculateBearing;
import static slash.navigation.googlemaps.GoogleUtil.unmarshalGeocode;

/**
 * Encapsulates REST access to the Google Geocoding API Service.
 *
 * @author Christian Pesch
 */

public class GoogleGeocodingService extends BaseGeocodingService {
    private static final Logger log = Logger.getLogger(GoogleGeocodingService.class.getName());
    private final GoogleApiClient apiClient = new GoogleApiClient();

    public String getName() {
        return "Google";
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isOverQueryLimit() {
        return apiClient.isOverQueryLimit();
    }

    private String getGeocodingUrl(String payload) {
        return apiClient.getGoogleApiUrl("geocode", payload);
    }

    private void checkForError(String url, String status) throws ServiceUnavailableException {
        apiClient.checkForError(getClass().getSimpleName(), url, status);
    }

    public String getAddressFor(NavigationPosition position) throws IOException {
        String url = getGeocodingUrl("latlng=" + position.getLatitude() + "," + position.getLongitude());
        Get get = apiClient.get(url);
        log.info("Getting location for " + position.getLongitude() + "," + position.getLatitude());
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                GeocodeResponse geocodeResponse = unmarshalGeocode(result);
                if (geocodeResponse != null) {
                    String status = geocodeResponse.getStatus();
                    checkForError(url, status);
                    return extractClosestLocation(geocodeResponse.getResult(), position.getLongitude(), position.getLatitude());
                }
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    private String extractClosestLocation(List<GeocodeResponse.Result> results,
                                          final double longitude, final double latitude) {
        List<String> locations = results.stream()
                .filter(r -> r.getType().stream().noneMatch(t -> t.equals("plus_code")))
                .sorted((p1, p2) -> {
                    GeocodeResponse.Result.Geometry.Location l1 = p1.getGeometry().getLocation();
                    GeocodeResponse.Result.Geometry.Location l2 = p2.getGeometry().getLocation();
                    double distance1 = calculateBearing(longitude, latitude, l1.getLng().doubleValue(), l1.getLat().doubleValue()).getDistance();
                    double distance2 = calculateBearing(longitude, latitude, l2.getLng().doubleValue(), l2.getLat().doubleValue()).getDistance();
                    return (int) (distance1 - distance2);
                })
                .map(GeocodeResponse.Result::getFormattedAddress)
                .toList();
        return !locations.isEmpty() ? locations.getFirst() : null;
    }

    public List<GeocodingResult> getPositionsFor(String address) throws IOException {
        String url = getGeocodingUrl("address=" + encodeUri(address));
        Get get = apiClient.get(url);
        log.info("Getting positions for " + address);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                GeocodeResponse geocodeResponse = unmarshalGeocode(result);
                if (geocodeResponse != null) {
                    String status = geocodeResponse.getStatus();
                    checkForError(url, status);
                    return asGeocodingResults(extractAdresses(geocodeResponse.getResult()));
                }
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    private List<CategorizedNavigationPosition> extractAdresses(List<GeocodeResponse.Result> responses) {
        List<CategorizedNavigationPosition> result = new ArrayList<>(responses.size());
        for (GeocodeResponse.Result response : responses) {
            result.add(extractAddress(response));
        }
        return result;
    }

    CategorizedNavigationPosition extractAddress(GeocodeResponse.Result response) {
        GeocodeResponse.Result.Geometry.Location location = response.getGeometry().getLocation();
        String type = response.getType().isEmpty() ? null : response.getType().get(0);
        return new SimpleCategorizedNavigationPosition(location.getLng().doubleValue(), location.getLat().doubleValue(),
                null, response.getFormattedAddress(), type);
    }
}
