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
import slash.navigation.elevation.ElevationService;

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
    private static final String ELEVATION_SERVICE = "elevationService-2.30"; // versioned preference

    private final List<ElevationService> elevationServices = new ArrayList<>();
    private ElevationService preferredElevationService;
    private boolean loggedFailedWarning;

    public void addElevationService(ElevationService elevationService) {
        ElevationService previous = findElevationService(elevationService.getName());
        if(previous != null) {
            elevationServices.set(elevationServices.indexOf(previous), elevationService);
        } else {
            elevationServices.add(elevationService);
            log.fine(format("Added elevation service '%s'", elevationService.getName()));
        }
    }

    public void setPreferredElevationService(ElevationService preferredElevationService) {
        this.preferredElevationService = preferredElevationService;
    }

    public List<ElevationService> getElevationServices() {
        return elevationServices;
    }

    public ElevationService findElevationService(String elevationServiceName) {
        for (ElevationService service : getElevationServices()) {
            if (elevationServiceName.endsWith(service.getName()))
                return service;
        }
        return null;
    }

    public ElevationService getElevationService() {
        String lookupServiceName = preferences.get(ELEVATION_SERVICE, preferredElevationService.getName());

        ElevationService service = findElevationService(lookupServiceName);
        if (service != null)
            return service;

        if (!loggedFailedWarning) {
            log.warning(format("Failed to find elevation service %s; using preferred %s", lookupServiceName, preferredElevationService.getName()));
            loggedFailedWarning = true;
        }
        return preferredElevationService;
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

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload) {
        getElevationService().downloadElevationDataFor(longitudeAndLatitudes, waitForDownload);
    }
}
