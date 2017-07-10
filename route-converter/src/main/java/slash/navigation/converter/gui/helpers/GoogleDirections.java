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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.mapview.MapView;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;

/**
 * Encapsulates access to Google Directions service.
 *
 * @author Christian Pesch
 */

public class GoogleDirections implements RoutingService {
    private static final Logger log = Logger.getLogger(GoogleDirections.class.getName());
    private static final TravelMode DRIVING = new TravelMode("Driving");
    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("Bicycling"), DRIVING, new TravelMode("Walking"));

    public String getName() {
        return "Google Directions";
    }

    public boolean isInitialized() {
        return true;
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isSupportTurnpoints() {
        return true;
    }

    public boolean isSupportAvoidFerries() {
        return true;
    }

    public boolean isSupportAvoidHighways() {
        return true;
    }

    public boolean isSupportAvoidTolls() {
        return true;
    }

    public List<TravelMode> getAvailableTravelModes() {
        return TRAVEL_MODES;
    }

    public TravelMode getPreferredTravelMode() {
        return DRIVING;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        throw new UnsupportedOperationException();
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        throw new UnsupportedOperationException();
    }

    public long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes) {
        throw new UnsupportedOperationException();
    }

    public void downloadRoutingData(List<BoundingBox> boundingBoxes) {
        throw new UnsupportedOperationException();
    }

    private Method findMethod(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredMethod(name, int[].class);
        } catch (NoSuchMethodException e) {
            Class<?> superclass = clazz.getSuperclass();
            if(superclass != null)
                return findMethod(superclass, name);
        }
        return null;
    }

    private void insertWaypoints(String mode, int[] selectedRows) {
        MapView mapView = RouteConverter.getInstance().getMapView();
        try {
            Method method = findMethod(mapView.getClass(), mode);
            if (method != null)
                method.invoke(mapView, new Object[]{selectedRows});
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.severe("Failed to " + mode + ": " + getLocalizedMessage(e));
        }
    }

    public void insertAllWaypoints(int[] selectedRows) {
        insertWaypoints("insertAllWaypoints", selectedRows);
    }

    public void insertOnlyTurnpoints(int[] selectedRows) {
        insertWaypoints("insertOnlyTurnpoints", selectedRows);
    }
}
