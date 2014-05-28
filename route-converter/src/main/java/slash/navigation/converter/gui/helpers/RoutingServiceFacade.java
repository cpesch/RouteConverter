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

import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;

/**
 * Helps to route between positions.
 *
 * @author Christian Pesch
 */

public class RoutingServiceFacade {
    private static final Logger log = Logger.getLogger(RoutingServiceFacade.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(RoutingServiceFacade.class);
    private static final String ROUTING_SERVICE_PREFERENCE = "routingService";
    private static final String TRAVEL_MODE_PREFERENCE = "travelMode";
    private static final String AVOID_FERRIES_PREFERENCE = "avoidFerries";
    private static final String AVOID_HIGHWAYS_PREFERENCE = "avoidHighways";
    private static final String AVOID_TOLLS_PREFERENCE = "avoidTolls";

    private final List<RoutingService> routingServices = new ArrayList<RoutingService>();
    private final EventListenerList listenerList = new EventListenerList();
    private boolean loggedFailedWarning = false;

    public List<RoutingService> getRoutingServices() {
        return routingServices;
    }

    public void addRoutingService(RoutingService routingService) {
        routingServices.add(0, routingService);
    }

    public RoutingService getRoutingService() {
        RoutingService firstRoutingService = routingServices.get(0);
        String lookupServiceName = preferences.get(ROUTING_SERVICE_PREFERENCE, firstRoutingService.getName());

        for (RoutingService service : routingServices) {
            if (lookupServiceName.endsWith(service.getName()))
                return service;
        }

        if(!loggedFailedWarning) {
            log.warning(format("Failed to find routing service %s; using first", lookupServiceName));
            loggedFailedWarning = true;
        }
        return firstRoutingService;
    }

    public void setRoutingService(RoutingService service) {
        preferences.put(ROUTING_SERVICE_PREFERENCE, service.getName());
        fireChanged();
    }

    public TravelMode getTravelMode() {
        List<TravelMode> travelModes = getRoutingService().getAvailableTravelModes();
        String lookupName = preferences.get(TRAVEL_MODE_PREFERENCE + getRoutingService().getName(), travelModes.get(0).getName());

        for (TravelMode travelMode : travelModes) {
            if (lookupName.equals(travelMode.getName()))
                return travelMode;
        }

        log.warning(format("Failed to find travel mode %s; using first", lookupName));
        return travelModes.get(0);
    }

    public void setTravelMode(TravelMode travelMode) {
        preferences.put(TRAVEL_MODE_PREFERENCE + getRoutingService().getName(), travelMode.getName());
        fireChanged();
    }

    public boolean isAvoidFerries() {
        return preferences.getBoolean(AVOID_FERRIES_PREFERENCE + getRoutingService().getName(), true);
    }

    public void setAvoidFerries(boolean avoidFerries) {
        preferences.putBoolean(AVOID_FERRIES_PREFERENCE + getRoutingService().getName(), avoidFerries);
        fireChanged();
    }

    public boolean isAvoidHighways() {
        return preferences.getBoolean(AVOID_HIGHWAYS_PREFERENCE + getRoutingService().getName(), true);
    }

    public void setAvoidHighways(boolean avoidHighways) {
        preferences.putBoolean(AVOID_HIGHWAYS_PREFERENCE + getRoutingService().getName(), avoidHighways);
        fireChanged();
    }

    public boolean isAvoidTolls() {
        return preferences.getBoolean(AVOID_TOLLS_PREFERENCE + getRoutingService().getName(), true);
    }

    public void setAvoidTolls(boolean avoidTolls) {
        preferences.putBoolean(AVOID_TOLLS_PREFERENCE + getRoutingService().getName(), avoidTolls);
        fireChanged();
    }

    protected void fireChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(null);
            }
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }
}
