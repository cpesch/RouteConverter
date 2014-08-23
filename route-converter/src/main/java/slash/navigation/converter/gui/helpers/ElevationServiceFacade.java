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
import slash.navigation.earthtools.EarthToolsService;
import slash.navigation.elevation.ElevationService;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.navigation.common.NavigationConversion.formatElevation;

/**
 * Helps to complement positions with elevation.
 *
 * @author Christian Pesch
 */

public class ElevationServiceFacade {
    private static final Logger log = Logger.getLogger(ElevationServiceFacade.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(ElevationServiceFacade.class);
    private static final String ELEVATION_SERVICE = "elevationService";

    private final List<ElevationService> elevationServices = new ArrayList<>();
    private boolean loggedFailedWarning = false;

    public ElevationServiceFacade() {
        elevationServices.add(new GeoNamesService());
        elevationServices.add(new GoogleMapsService());
        elevationServices.add(new EarthToolsService());
    }

    public void addElevationService(ElevationService elevationService) {
        elevationServices.add(0, elevationService);
    }

    public List<ElevationService> getElevationServices() {
        return elevationServices;
    }

    public ElevationService getElevationService() {
        ElevationService firstElevationService = elevationServices.get(0);
        String lookupServiceName = preferences.get(ELEVATION_SERVICE, firstElevationService.getName());

        for (ElevationService service : elevationServices) {
            if (lookupServiceName.endsWith(service.getName()))
                return service;
        }

        if (!loggedFailedWarning) {
            log.warning(format("Failed to find elevation service %s; using first %s", lookupServiceName, firstElevationService.getName()));
            loggedFailedWarning = true;
        }
        return firstElevationService;
    }

    public void setElevationService(ElevationService service) {
        preferences.put(ELEVATION_SERVICE, service.getName());
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Double elevation = getElevationService().getElevationFor(longitude, latitude);
        return elevation != null ? formatElevation(elevation).doubleValue() : null;
    }

    public boolean isDownload() {
        return getElevationService().isDownload();
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        getElevationService().downloadElevationDataFor(longitudeAndLatitudes);
    }
}
