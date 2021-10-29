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

import slash.navigation.routing.RoutingPreferencesModel;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.RoutingServiceListener;

import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Helps to route between positions.
 *
 * @author Christian Pesch
 */

public class RoutingServiceFacade {
    private static final Logger log = Logger.getLogger(RoutingServiceFacade.class.getName());

    private final RoutingPreferencesModel routingPreferencesModel = new RoutingPreferencesModel();
    private final EventListenerList listenerList = new EventListenerList();

    public RoutingPreferencesModel getRoutingPreferencesModel() {
        return routingPreferencesModel;
    }

    public void addRoutingService(RoutingService routingService) {
        routingPreferencesModel.addRoutingService(routingService);
        routingService.addRoutingServiceListener(new RoutingServiceEventForwarder());
        log.fine(format("Added routing service '%s'", routingService.getName()));
    }

    public void setPreferredRoutingService(RoutingService preferredRoutingService) {
        routingPreferencesModel.setPreferredRoutingService(preferredRoutingService);
    }

    public RoutingService getRoutingService() {
        return routingPreferencesModel.getRoutingService();
    }

    public void setRoutingService(RoutingService service) {
        routingPreferencesModel.setRoutingService(service);
    }

    @SuppressWarnings("unchecked")
    public <T> T getRoutingService(Class<T> clazz) {
        for (RoutingService service : routingPreferencesModel.getRoutingServices()) {
            if (service.getClass().equals(clazz))
                return (T) service;
        }
        return null;
    }

    public void addPreferencesChangeListener(ChangeListener l) {
        routingPreferencesModel.addChangeListener(l);
    }

    private void fireDownloading() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RoutingServiceListener.class) {
                ((RoutingServiceListener) listeners[i + 1]).downloading();
            }
        }
    }

    private void fireInitializing(int second) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RoutingServiceListener.class) {
                ((RoutingServiceListener) listeners[i + 1]).processing(second);
            }
        }
    }

    private void fireRouting(int second) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RoutingServiceListener.class) {
                ((RoutingServiceListener) listeners[i + 1]).routing(second);
            }
        }
    }

    public void addRoutingServiceFacadeListener(RoutingServiceListener l) {
        listenerList.add(RoutingServiceListener.class, l);
    }

    private class RoutingServiceEventForwarder implements RoutingServiceListener {
        public void downloading() {
            fireDownloading();
        }

        public void processing(int second) {
            fireInitializing(second);
        }

        public void routing(int second) {
            fireRouting(second);
        }
    }
}
