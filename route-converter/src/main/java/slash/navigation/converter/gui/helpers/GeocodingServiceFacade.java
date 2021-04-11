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

import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.GeocodingService;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;

/**
 * Helps to convert addresses into geographic coordinates.
 *
 * @author Christian Pesch
 */

public class GeocodingServiceFacade {
    private static final Logger log = Logger.getLogger(GeocodingServiceFacade.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GeocodingServiceFacade.class);
    private static final String GEOCODING_SERVICE = "geocodingService-2.30"; // versioned preference

    private final List<GeocodingService> geocodingServices = new ArrayList<>();
    private GeocodingService preferredGeocodingService;
    private boolean loggedFailedWarning;

    public void addGeocodingService(GeocodingService geocodingService) {
        GeocodingService previous = findGeocodingService(geocodingService.getName());
        if(previous != null) {
            geocodingServices.set(geocodingServices.indexOf(previous), geocodingService);
        } else {
            geocodingServices.add(geocodingService);
            log.info(format("Added geocoding service '%s'", geocodingService.getName()));
        }
    }

    public void setPreferredGeocodingService(GeocodingService preferredGeocodingService) {
        this.preferredGeocodingService = preferredGeocodingService;
    }

    public List<GeocodingService> getGeocodingServices() {
        return geocodingServices;
    }

    public GeocodingService findGeocodingService(String geocodingServiceName) {
        for (GeocodingService service : getGeocodingServices()) {
            if (geocodingServiceName.endsWith(service.getName()))
                return service;
        }
        return null;
    }

    public GeocodingService getGeocodingService() {
        String lookupServiceName = preferences.get(GEOCODING_SERVICE, preferredGeocodingService.getName());

        GeocodingService service = findGeocodingService(lookupServiceName);
        if (service != null)
            return service;

        if (!loggedFailedWarning) {
            log.warning(format("Failed to find geocoding service %s; using preferred %s", lookupServiceName, preferredGeocodingService.getName()));
            loggedFailedWarning = true;
        }
        return preferredGeocodingService;
    }

    public void setGeocodingService(GeocodingService service) {
        preferences.put(GEOCODING_SERVICE, service.getName());
    }

    public List<NavigationPosition> getPositionsFor(String address) throws IOException, ServiceUnavailableException {
        return getGeocodingService().getPositionsFor(address);
    }

    public String getAddressFor(NavigationPosition position) throws IOException, ServiceUnavailableException {
        return getGeocodingService().getAddressFor(position);
    }

    public NavigationPosition getPositionFor(String address) throws IOException, ServiceUnavailableException {
        List<NavigationPosition> positions = getPositionsFor(address);
        return positions != null && positions.size() > 0 ? positions.get(0) : null;
    }
}
