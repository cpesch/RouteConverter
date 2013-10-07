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

package slash.navigation.completer;

import slash.navigation.earthtools.EarthToolsService;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.hgt.HgtFiles;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static slash.navigation.common.NavigationConversion.formatElevation;

/**
 * Helps to complement positions with elevation, postal address and populated place information.
 *
 * @author Christian Pesch
 */

public class CompletePositionService {
    private static final Logger log = Logger.getLogger(CompletePositionService.class.getName());
    protected static final Preferences preferences = Preferences.userNodeForPackage(CompletePositionService.class);
    private static final String COMPLEMENT_ELEVATION_FROM_HGT_FILES = "complementElevationFromHgtFiles";
    private static final String COMPLEMENT_ELEVATION_FROM_GOOGLE_MAPS = "complementElevationFromGoogleMaps";
    private static final String COMPLEMENT_ELEVATION_FROM_GEONAMES = "complementElevationFromGeonames";
    private static final String COMPLEMENT_ELEVATION_FROM_EARTH_TOOLS = "complementElevationFromEarthTools";

    private EarthToolsService earthToolsService = new EarthToolsService();
    private GeoNamesService geoNamesService = new GeoNamesService();
    private GoogleMapsService googleMapsService = new GoogleMapsService();
    private HgtFiles hgtFiles = new HgtFiles();

    public void dispose() {
        hgtFiles.dispose();
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Double elevation = null;
        Exception exception = null;

        if (preferences.getBoolean(COMPLEMENT_ELEVATION_FROM_HGT_FILES, true)) {
            try {
                elevation = hgtFiles.getElevationFor(longitude, latitude);
                log.info("Service: HGTFiles Longitude: " + longitude + " Latitude: " + latitude + " Elevation: " + elevation);
            } catch (Exception e) {
                exception = e;
            }
        }
        if (elevation == null && preferences.getBoolean(COMPLEMENT_ELEVATION_FROM_GOOGLE_MAPS, true)) {
            try {
                elevation = googleMapsService.getElevationFor(longitude, latitude);
                log.info("Service: GoogleMaps Longitude: " + longitude + " Latitude: " + latitude + " Elevation: " + elevation);
            } catch (Exception e) {
                exception = e;
            }
        }
        if (elevation == null && preferences.getBoolean(COMPLEMENT_ELEVATION_FROM_GEONAMES, true)) {
            try {
                elevation = geoNamesService.getElevationFor(longitude, latitude);
                log.info("Service: GeoNames Longitude: " + longitude + " Latitude: " + latitude + " Elevation: " + elevation);
            } catch (Exception e) {
                exception = e;
            }
        }
        if (elevation == null && preferences.getBoolean(COMPLEMENT_ELEVATION_FROM_EARTH_TOOLS, true)) {
            try {
                elevation = earthToolsService.getElevationFor(longitude, latitude);
                log.info("Service: EarthTools Longitude: " + longitude + " Latitude: " + latitude + " Elevation: " + elevation);
            } catch (Exception e) {
                exception = e;
            }
        }

        if(elevation != null)
            return formatElevation(elevation).doubleValue();
        if(exception != null)
            throw new IOException(exception);
        return null;
    }

    public String getDescriptionFor(double longitude, double latitude) throws IOException {
        String description = googleMapsService.getLocationFor(longitude, latitude);
        if (description == null)
            description = geoNamesService.getNearByFor(longitude, latitude);
        return description;
    }
}
