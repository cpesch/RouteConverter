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

import slash.navigation.brouter.BRouter;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.RoutingService;
import slash.navigation.graphhopper.GraphHopper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;

/**
 * Helps to insert positions.
 *
 * @author Christian Pesch
 */

public class RoutingServiceFacade {
    private static final Logger log = Logger.getLogger(RoutingServiceFacade.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(RoutingServiceFacade.class);
    private static final String ROUTING_SERVICE = "routingService";

    private final List<RoutingService> routingServices = new ArrayList<RoutingService>();

    public RoutingServiceFacade(DownloadManager downloadManager) {
        routingServices.add(new BRouter(downloadManager));
        routingServices.add(new GraphHopper(downloadManager));
    }

    public List<RoutingService> getRoutingServices() {
        return routingServices;
    }

    public RoutingService getRoutingService() {
        String lookupServiceName = preferences.get(ROUTING_SERVICE, routingServices.get(0).getName());

        for (RoutingService service : routingServices) {
            if (lookupServiceName.endsWith(service.getName()))
                return service;
        }

        log.warning(format("Failed to find routing service %s; using first", lookupServiceName));
        return routingServices.get(0);
    }

    public void setRoutingService(RoutingService service) {
        preferences.put(ROUTING_SERVICE, service.getName());
    }
}
