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

/**
 * Helps to complete positions with elevation, postal address and populated place information.
 *
 * @author Christian Pesch
 */

public class CompletePositionService {
    private EarthToolsService earthToolsService = new EarthToolsService();
    private GeoNamesService geoNamesService = new GeoNamesService();
    private GoogleMapsService googleMapsService = new GoogleMapsService();
    private HgtFiles hgtFiles = new HgtFiles();

    public void dispose() {
        hgtFiles.dispose();
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Double elevation = hgtFiles.getElevationFor(longitude, latitude);
        if (elevation == null)
            elevation = googleMapsService.getElevationFor(longitude, latitude);
        if (elevation == null)
            elevation = geoNamesService.getElevationFor(longitude, latitude);
        if (elevation == null)
            elevation = earthToolsService.getElevationFor(longitude, latitude);
        return elevation;
    }

    public String getCommentFor(double longitude, double latitude) throws IOException {
        String comment = googleMapsService.getLocationFor(longitude, latitude);
        if (comment == null)
            comment = geoNamesService.getNearByFor(longitude, latitude);
        return comment;
    }
}
