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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.geonames;

import slash.navigation.rest.Get;
import slash.navigation.geonames.binding.Geonames;
import slash.navigation.util.Conversion;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Encapsulates REST access to the GeoNames.org service.
 *
 * @author Christian Pesch
 */

public class GeoNamesService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoNamesService.class);
    private static final String GEONAMES_URL_PREFERENCE = "geonamesUrl";

    private static String getGeoNamesUrlPreference() {
        return preferences.get(GEONAMES_URL_PREFERENCE, "http://ws.geonames.org/");
    }

    private Integer getElevationFor(String uri, double longitude, double latitude, Integer nullValue) throws IOException {
        Get get = new Get(getGeoNamesUrlPreference() + uri + "?lat=" + latitude + "&lng=" + longitude);
        String result = get.execute();
        if (get.isSuccessful())
            try {
                Integer elevation = Conversion.parseInt(result);
                if (elevation != null && !elevation.equals(nullValue))
                    return elevation;
            } catch (NumberFormatException e) {
                IOException io = new IOException("Cannot unmarshall " + result + ": " + e.getMessage());
                io.setStackTrace(e.getStackTrace());
                throw io;
            }
        return null;
    }

    Integer getSrtm3ElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("srtm3", longitude, latitude, -32768);
    }

    Integer getGtopo30ElevationFor(double longitude, double latitude) throws IOException {
        return getElevationFor("gtopo30", longitude, latitude, -9999);
    }

    public Integer getElevationFor(double longitude, double latitude) throws IOException {
        if (latitude < 60.0 && latitude > -56.0)
            return getSrtm3ElevationFor(longitude, latitude);
        else
            return getGtopo30ElevationFor(longitude, latitude);
    }

    private String getNearByFor(String uri, double longitude, double latitude) throws IOException {
        Get get = new Get(getGeoNamesUrlPreference() + uri + "?lat=" + latitude + "&lng=" + longitude);
        String result = get.execute();
        if (get.isSuccessful())
            try {
                Geonames geonames = GeoNamesUtil.unmarshal(result);
                if (geonames.getGeoname() != null)
                    return geonames.getGeoname().getName();
            } catch (JAXBException e) {
                IOException io = new IOException("Cannot unmarshall " + result + ": " + e.getMessage());
                io.setStackTrace(e.getStackTrace());
                throw io;
            }
        return null;
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
}
