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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.elevation.ElevationService;
import slash.navigation.googlemaps.elevation.ElevationResponse;
import slash.navigation.googlemaps.geocode.GeocodeResponse;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static java.util.Arrays.sort;
import static slash.common.io.Transfer.encodeUri;
import static slash.navigation.common.Bearing.calculateBearing;
import static slash.navigation.googlemaps.GoogleMapsAPIKey.getAPIKey;
import static slash.navigation.googlemaps.GoogleMapsServer.getGoogleMapsServer;
import static slash.navigation.googlemaps.GoogleMapsUtil.unmarshalElevation;
import static slash.navigation.googlemaps.GoogleMapsUtil.unmarshalGeocode;
import static slash.navigation.rest.HttpRequest.USER_AGENT;

/**
 * Encapsulates REST access to the Google Maps API Geocoding Service.
 *
 * @author Christian Pesch
 */

public class GoogleMapsService implements ElevationService {
    private static final Logger log = Logger.getLogger(GoogleMapsService.class.getName());

    public String getName() {
        return "Google Maps";
    }

    private String getGoogleMapsApiUrl(String api, String payload) {
        String language = Locale.getDefault().getLanguage();
        return getGoogleMapsServer().getApiUrl() + "/maps/api/" + api + "/xml?" + payload +
                "&sensor=false&language=" + language + "&key=" + getAPIKey(api);
    }

    private String getElevationUrl(String payload) {
        return getGoogleMapsApiUrl("elevation", payload);
    }

    private String getGeocodingUrl(String payload) {
        return getGoogleMapsApiUrl("geocode", payload);
    }

    private Get get(String url) {
        Get get = new Get(url);
        get.setUserAgent(USER_AGENT);
        return get;
    }

    private void checkForError(String url, String status) throws ServiceUnavailableException {
        if (!status.equals("OK"))
            throw new ServiceUnavailableException(getClass().getSimpleName(), url, status);
    }

    public String getLocationFor(double longitude, double latitude) throws IOException {
        String url = getGeocodingUrl("latlng=" + latitude + "," + longitude);
        Get get = get(url);
        log.info("Getting location for " + longitude + "," + latitude);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                GeocodeResponse geocodeResponse = unmarshalGeocode(result);
                if (geocodeResponse != null) {
                    String status = geocodeResponse.getStatus();
                    checkForError(url, status);
                    return extractClosestLocation(geocodeResponse.getResult(), longitude, latitude);
                }
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    private String extractClosestLocation(List<GeocodeResponse.Result> results,
                                          final double longitude, final double latitude) {
        GeocodeResponse.Result[] resultsArray = results.toArray(new GeocodeResponse.Result[results.size()]);
        sort(resultsArray, new Comparator<GeocodeResponse.Result>() {
            public int compare(GeocodeResponse.Result p1, GeocodeResponse.Result p2) {
                GeocodeResponse.Result.Geometry.Location l1 = p1.getGeometry().getLocation();
                GeocodeResponse.Result.Geometry.Location l2 = p2.getGeometry().getLocation();
                double distance1 = calculateBearing(longitude, latitude, l1.getLng().doubleValue(), l1.getLat().doubleValue()).getDistance();
                double distance2 = calculateBearing(longitude, latitude, l2.getLng().doubleValue(), l2.getLat().doubleValue()).getDistance();
                return (int) (distance1 - distance2);
            }
        });
        return resultsArray[0].getFormattedAddress();
    }

    public NavigationPosition getPositionFor(String address) throws IOException {
        List<NavigationPosition> positions = getPositionsFor(address);
        return positions != null && positions.size() > 0 ? positions.get(0) : null;
    }

    public List<NavigationPosition> getPositionsFor(String address) throws IOException {
        String url = getGeocodingUrl("address=" + encodeUri(address));
        Get get = get(url);
        log.info("Getting positions for " + address);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                GeocodeResponse geocodeResponse = unmarshalGeocode(result);
                if (geocodeResponse != null) {
                    String status = geocodeResponse.getStatus();
                    checkForError(url, status);
                    return extractAdresses(geocodeResponse.getResult());
                }
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    private List<NavigationPosition> extractAdresses(List<GeocodeResponse.Result> responses) {
        List<NavigationPosition> result = new ArrayList<>(responses.size());
        for (GeocodeResponse.Result response : responses) {
            GeocodeResponse.Result.Geometry.Location location = response.getGeometry().getLocation();
            result.add(new SimpleNavigationPosition(location.getLng().doubleValue(), location.getLat().doubleValue(),
                    0.0d, response.getFormattedAddress()));
        }
        return result;
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        String url = getElevationUrl("locations=" + latitude + "," + longitude); // could be up to 512 locations
        Get get = get(url);
        log.info("Getting elevation for " + longitude + "," + latitude);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                ElevationResponse elevationResponse = unmarshalElevation(result);
                if (elevationResponse != null) {
                    String status = elevationResponse.getStatus();
                    checkForError(url, status);
                    List<Double> elevations = extractElevations(elevationResponse.getResult());
                    return elevations != null && elevations.size() > 0 ? elevations.get(0) : null;
                }
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    private List<Double> extractElevations(List<ElevationResponse.Result> responses) {
        List<Double> results = new ArrayList<>(responses.size());
        for (ElevationResponse.Result response : responses) {
            results.add(response.getElevation().doubleValue());
        }
        return results;
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isSupportsPath() {
        return false;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload) {
        throw new UnsupportedOperationException();
    }

    public long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes) {
        throw new UnsupportedOperationException();
    }

    public void downloadElevationData(List<BoundingBox> boundingBoxes) {
        throw new UnsupportedOperationException();
    }
}
