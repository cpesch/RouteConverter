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

import slash.common.helpers.APIKeyRegistry;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.elevation.ElevationService;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.geonames.binding.Geonames;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.parseInteger;
import static slash.common.io.Transfer.trim;

/**
 * Encapsulates REST access to the geonames.org service.
 *
 * @author Christian Pesch
 */

public class GeoNamesService implements ElevationService, GeocodingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoNamesService.class);
    private static final Logger log = Logger.getLogger(GeoNamesService.class.getName());
    private static final String GEONAMES_URL_PREFERENCE = "geonamesUrl";
    private int overQueryLimitCount;

    public String getName() {
        return "GeoNames";
    }

    public boolean isOverQueryLimit() {
        return overQueryLimitCount > 0;
    }

    private String getGeoNamesApiUrl() {
        return preferences.get(GEONAMES_URL_PREFERENCE, "http://api.geonames.org/");
    }

    private String execute(String uri, String apiType) throws IOException {
        String userName = trim(APIKeyRegistry.getInstance().getAPIKey("geonames", apiType));
        if(userName == null)
            return null;

        String url = getGeoNamesApiUrl() + uri + "&username=" + userName;
        Get get = new Get(url);
        String result = get.executeAsString();
        if (get.isSuccessful()) {
            checkCurrentlyOverloaded(url, result);
            return result;
        }
        return null;
    }

    private Integer getElevationFor(String uri, double longitude, double latitude, Integer nullValue) throws IOException {
        String result = execute(uri + "?lat=" + latitude + "&lng=" + longitude, uri); // could be up to 20 points
        if (result != null) {
            try {
                Integer elevation = parseInteger(result);
                if (elevation != null && !elevation.equals(nullValue))
                    return elevation;
            } catch (NumberFormatException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    private void checkCurrentlyOverloaded(String url, String result) throws ServiceUnavailableException {
        if (result.contains("limit") && (result.contains("overloaded") || result.contains("exceeded"))) {
            overQueryLimitCount++;
            log.warning("geonames API is over query limit, count: " + overQueryLimitCount + ", url: " + url);
            throw new ServiceUnavailableException(getClass().getSimpleName(), url, result);
        }
    }

    Integer getAsterGDEMElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("astergdem", longitude, latitude, -9999);
    }

    Integer getSRTM3ElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("srtm3", longitude, latitude, -32768);
    }

    Integer getGTOPO30ElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("gtopo30", longitude, latitude, -9999);
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Integer elevation = null;

        if (latitude < 83.0 && latitude > -65.0)
            elevation = getAsterGDEMElevationFor(longitude, latitude);

        if (elevation == null && latitude < 60.0 && latitude > -56.0)
            elevation = getSRTM3ElevationFor(longitude, latitude);

        if (elevation == null)
            elevation = getGTOPO30ElevationFor(longitude, latitude);

        return elevation != null ? elevation.doubleValue() : null;
    }

    public List<NavigationPosition> getPositionsFor(String address) throws IOException {
        return null; // not supported
    }

    private Geonames getGeonamesFor(String uri, String apiType) throws IOException {
        String result = execute(uri, apiType);
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
        return getGeonamesFor(uri + "?lat=" + latitude + "&lng=" + longitude, uri);
    }

    private String getNearByFor(String uri, double longitude, double latitude) throws IOException {
        Geonames geonames = getGeonamesFor(uri, longitude, latitude);
        if (geonames == null || geonames.getGeoname() == null)
            return null;
        if (geonames.getStatus() != null)
            throw new IOException(geonames.getStatus().getMessage());
        List<String> result = new ArrayList<>();
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

    public String getAddressFor(NavigationPosition position) throws IOException {
        String description = getNearByPlaceNameFor(position.getLongitude(), position.getLatitude());
        if (description == null)
            description = getNearByToponymFor(position.getLongitude(), position.getLatitude());
        return description;
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
