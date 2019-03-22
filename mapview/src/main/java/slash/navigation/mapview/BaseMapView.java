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

package slash.navigation.mapview;

import slash.navigation.common.DistanceAndTime;

import javax.swing.event.EventListenerList;
import java.util.Map;

/**
 * The base of all {@link MapView} implementations.
 *
 * @author Christian Pesch
 */

public abstract class BaseMapView implements MapView {
    protected static final String CENTER_LATITUDE_PREFERENCE = "centerLatitude";
    protected static final String CENTER_LONGITUDE_PREFERENCE = "centerLongitude";
    protected static final String CENTER_ZOOM_PREFERENCE = "centerZoom";

    private EventListenerList listenerList = new EventListenerList();

    protected void fireCalculatedDistances(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MapViewListener.class) {
                ((MapViewListener) listeners[i + 1]).calculatedDistances(indexToDistanceAndTime);
            }
        }
    }

    protected void fireReceivedCallback(int port) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MapViewListener.class) {
                ((MapViewListener) listeners[i + 1]).receivedCallback(port);
            }
        }
    }

    public void addMapViewListener(MapViewListener l) {
        listenerList.add(MapViewListener.class, l);
    }

    public void removeMapViewListener(MapViewListener l) {
        listenerList.remove(MapViewListener.class, l);
    }
}
