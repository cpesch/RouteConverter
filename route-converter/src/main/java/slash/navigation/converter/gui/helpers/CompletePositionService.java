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

package slash.navigation.converter.gui.helpers;

import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.download.DownloadManager;
import slash.navigation.earthtools.EarthToolsService;
import slash.navigation.elevation.ElevationService;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.hgt.HgtFiles;
import slash.navigation.hgt.HgtFilesService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.navigation.common.NavigationConversion.formatElevation;

/**
 * Helps to complement positions with elevation, postal address and populated place information.
 *
 * @author Christian Pesch
 */

public class CompletePositionService implements ElevationService {
    private static final Logger log = Logger.getLogger(CompletePositionService.class.getName());
    protected static final Preferences preferences = Preferences.userNodeForPackage(CompletePositionService.class);
    private static final String ELEVATION_SERVICE = "elevationService";

    private final List<ElevationService> elevationServices = new ArrayList<ElevationService>();
    private final HgtFilesService hgtFilesService;
    private final GeoNamesService geoNamesService = new GeoNamesService();
    private final GoogleMapsService googleMapsService = new GoogleMapsService();

    public CompletePositionService(DownloadManager downloadManager) {
        hgtFilesService = new HgtFilesService(downloadManager);
        for(HgtFiles hgtFile : hgtFilesService.getHgtFiles())
            elevationServices.add(hgtFile);
        elevationServices.add(geoNamesService);
        elevationServices.add(googleMapsService);
        elevationServices.add(new EarthToolsService());
    }

    public String getName() {
        return "Complete Position Facade";
    }

    public void dispose() {
        hgtFilesService.dispose();
    }

    public List<ElevationService> getElevationServices() {
        return elevationServices;
    }

    public ElevationService getElevationService() {
        String lookupServiceName = preferences.get(ELEVATION_SERVICE, elevationServices.get(0).getName());

        for (ElevationService service : elevationServices) {
            if (lookupServiceName.endsWith(service.getName()))
                return service;
        }

        log.warning(format("Failed to find elevation service %s; using GeoNames", lookupServiceName));
        return geoNamesService;
    }

    public void setElevationService(ElevationService service) {
        preferences.put(ELEVATION_SERVICE, service.getName());
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Double elevation = getElevationService().getElevationFor(longitude, latitude);
        return elevation != null ? formatElevation(elevation).doubleValue() : null;
    }

    public String getDescriptionFor(double longitude, double latitude) throws IOException {
        String description = googleMapsService.getLocationFor(longitude, latitude);
        if (description == null)
            description = geoNamesService.getNearByFor(longitude, latitude);
        return description;
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        getElevationService().downloadElevationDataFor(longitudeAndLatitudes);
    }
}
