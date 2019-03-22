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

package slash.navigation.routing;

import javax.swing.event.EventListenerList;

/**
 * The base of all {@link RoutingService} implementations.
 *
 * @author Christian Pesch
 */

public abstract class BaseRoutingService implements RoutingService {
    private EventListenerList listenerList = new EventListenerList();

    protected void fireDownloading() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RoutingServiceListener.class) {
                ((RoutingServiceListener) listeners[i + 1]).downloading();
            }
        }
    }

    protected void fireInitializing(int second) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RoutingServiceListener.class) {
                ((RoutingServiceListener) listeners[i + 1]).processing(second);
            }
        }
    }

    protected void fireRouting(int second) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == RoutingServiceListener.class) {
                ((RoutingServiceListener) listeners[i + 1]).routing(second);
            }
        }
    }

    public void addRoutingServiceListener(RoutingServiceListener l) {
        listenerList.add(RoutingServiceListener.class, l);
    }
}
