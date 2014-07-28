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

import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.elevation.ElevationService;
import slash.navigation.geonames.binding.Geonames;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.parseInt;

/**
 * Encapsulates REST access to the geonames.org service.
 *
 * @author Christian Pesch
 */

public class GeoNamesService implements ElevationService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoNamesService.class);
    private static final String GEONAMES_URL_PREFERENCE = "geonamesUrl";
    private static final String GEONAMES_USERNAME_PREFERENCE = "geonamesUserName";

    public String getName() {
        return "GeoNames";
    }

    private String getGeoNamesNamesUrl() {
        return preferences.get(GEONAMES_URL_PREFERENCE, "http://api.geonames.org/");
    }

    private String getGeoNamesUserName() {
        return preferences.get(GEONAMES_USERNAME_PREFERENCE, "routeconverter");
    }

    private String execute(String uri) throws IOException {
        String url = getGeoNamesNamesUrl() + uri + "&username=" + getGeoNamesUserName();
        Get get = new Get(url);
        String result = get.executeAsString();
        if (get.isSuccessful()) {
            checkCurrentlyOverloaded(url, result);
            return result;
        }
        return null;
    }

    private Integer getElevationFor(String uri, double longitude, double latitude, Integer nullValue) throws IOException {
        String result = execute(uri + "?lat=" + latitude + "&lng=" + longitude);
        if (result != null) {
            try {
                Integer elevation = parseInt(result);
                if (elevation != null && !elevation.equals(nullValue))
                    return elevation;
            } catch (NumberFormatException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    private void checkCurrentlyOverloaded(String url, String result) throws ServiceUnavailableException {
        if (result.contains("<html>") && (result.contains("overloaded") || result.contains("exceeded")))
            throw new ServiceUnavailableException("geonames.org", url);
    }

    Integer getSrtm3ElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("srtm3", longitude, latitude, -32768);
    }

    Integer getGtopo30ElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("gtopo30", longitude, latitude, -9999);
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        if (latitude < 60.0 && latitude > -56.0) {
            Integer elevation = getSrtm3ElevationFor(longitude, latitude);
            return elevation != null ? elevation.doubleValue() : null;
        } else {
            Integer elevation = getGtopo30ElevationFor(longitude, latitude);
            return elevation != null ? elevation.doubleValue() : null;
        }
    }

    private Geonames getGeonamesFor(String uri) throws IOException {
        String result = execute(uri);
        if (result != null) {
            try {
                return GeoNamesUtil.unmarshal(result);
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    private Geonames getGeonamesFor(String uri, double longitude, double latitude) throws IOException {
        return getGeonamesFor(uri + "?lat=" + latitude + "&lng=" + longitude);
    }

    private String getNearByFor(String uri, double longitude, double latitude) throws IOException {
        Geonames geonames = getGeonamesFor(uri, longitude, latitude);
        if (geonames == null || geonames.getGeoname() == null)
            return null;
        if (geonames.getStatus() != null)
            throw new IOException(geonames.getStatus().getMessage());
        List<String> result = new ArrayList<String>();
        for (Geonames.Geoname geoname : geonames.getGeoname()) {
            result.add(geoname.getName());
        }
        return result.size() > 0 ? result.get(0) : null;
    }

    String getNearByToponymFor(double longitude, double latitude) throws IOException {
        return getNearByFor("findNearby", longitude, latitude);
    }

    String getNearByPlaceNameFor(double longitude, double latitude) throws IOException {
        return getNearByFor("findNearbyPlaceName", longitude, latitude);
    }

    public String getNearByFor(double longitude, double latitude) throws IOException {
        String description = getNearByPlaceNameFor(longitude, latitude);
        if (description == null)
            description = getNearByToponymFor(longitude, latitude);
        return description;
    }

    public PostalCode getNearByPostalCodeFor(double longitude, double latitude) throws IOException {
        Geonames geonames = getGeonamesFor("findNearbyPostalCodes", longitude, latitude);
        if (geonames == null || geonames.getCode() == null)
            return null;
        List<PostalCode> result = new ArrayList<PostalCode>();
        for (Geonames.Code code : geonames.getCode()) {
            result.add(new PostalCode(code.getCountryCode(), code.getPostalcode(), code.getName()));
        }
        return result.size() > 0 ? result.get(0) : null;
    }

    public String getPlaceNameFor(String countryCode, String postalCode) throws IOException {
        Geonames geonames = getGeonamesFor("postalCodeSearch?postalcode=" + postalCode + "&country=" + countryCode);
        if (geonames == null || geonames.getCode() == null)
            return null;
        List<PostalCode> result = new ArrayList<PostalCode>();
        for (Geonames.Code code : geonames.getCode()) {
            result.add(new PostalCode(code.getCountryCode(), code.getPostalcode(), code.getName()));
        }
        return result.size() > 0 ? result.get(0).placeName : null;
    }

    /**
     * Return longitude and latitude for the given country and postal code.
     *
     * @param countryCode the country code to search a position for
     * @param postalCode  the postal code to search a position for
     * @return the longitude and latitude for the given country and postal code
     * @throws IOException if an error occurs while accessing geonames.org
     */
    public double[] getPositionFor(String countryCode, String postalCode) throws IOException {
        Geonames geonames = getGeonamesFor("postalCodeSearch?postalcode=" + postalCode + "&country=" + countryCode);
        if (geonames == null || geonames.getCode() == null)
            return null;
        List<Double> result = new ArrayList<Double>();
        for (Geonames.Code code : geonames.getCode()) {
            result.add(code.getLng().doubleValue());
            result.add(code.getLat().doubleValue());
        }
        return result.size() > 1 ? new double[]{result.get(0), result.get(1)} : null;
    }

    public boolean isDownload() {
        return false;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        throw new UnsupportedOperationException();
    }
}
