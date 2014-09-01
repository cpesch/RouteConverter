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
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.mapview.MapView;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Encapsulates access to Google Directions service.
 *
 * @author Christian Pesch
 */

public class GoogleDirections implements RoutingService {
    private static final TravelMode DRIVING = new TravelMode("Driving");
    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("Bicycling"), DRIVING, new TravelMode("Walking"));

    private final MapView mapView;

    public GoogleDirections(MapView mapView) {
        this.mapView = mapView;
    }

    public String getName() {
        return "Google Directions";
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isSupportTurnpoints() {
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

    public void insertAllWaypoints(int[] selectedRows) {
        mapView.insertAllWaypoints(selectedRows);
    }

    public void insertOnlyTurnpoints(int[] selectedRows) {
        mapView.insertOnlyTurnpoints(selectedRows);
    }
}
