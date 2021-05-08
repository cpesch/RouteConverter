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

import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.maps.tileserver.TileServerMapManager;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

/**
 * Interface for callbacks from the {@link MapView} to the other RouteConverter services.
 *
 * @author Christian Pesch
 */

public interface MapViewCallback {
    String createDescription(int index, String description);
    String createCoordinates(Double longitude, Double latitude);
    void setSelectedPositions(int[] selectedPositions, boolean replaceSelection);
    void complementData(int[] rows, boolean description, boolean time, boolean elevation, boolean waitForDownload, boolean trackUndo);
    void startBrowser(String url);

    RoutingService getRoutingService();
    TravelMode getTravelMode();
    boolean isAvoidFerries();
    boolean isAvoidHighways();
    boolean isAvoidTolls();

    boolean isShowAllPositionsAfterLoading();
    boolean isRecenterAfterZooming();

    TileServerMapManager getTileServerMapManager();
    DistanceAndTimeAggregator getDistanceAndTimeAggregator();
}
