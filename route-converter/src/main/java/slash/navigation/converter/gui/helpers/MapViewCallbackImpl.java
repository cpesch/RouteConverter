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

import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.maps.tileserver.TileServerMapManager;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import static slash.navigation.converter.gui.helpers.PositionHelper.formatLatitude;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatLongitude;

/**
 * Implements the common callbacks from the {@link MapView} for the RouteConverter services.
 *
 * @author Christian Pesch
 */

public abstract class MapViewCallbackImpl implements MapViewCallback {

    public String createDescription(int index, String description) {
        return RouteConverter.getInstance().getPositionAugmenter().createDescription(index, description);
    }

    public String createCoordinates(Double longitude, Double latitude) {
        return formatLongitude(longitude) + "," + formatLatitude(latitude);
    }

    public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        RouteConverter.getInstance().getConvertPanel().getPositionsSelectionModel().setSelectedPositions(selectedPositions, replaceSelection);
    }

    public void complementData(int[] rows, boolean description, boolean time, boolean elevation, boolean waitForDownload, boolean trackUndo) {
        RouteConverter.getInstance().getPositionAugmenter().addData(rows, description, time, elevation, waitForDownload, trackUndo);
    }

    public void startBrowser(String url) {
        ExternalPrograms.startBrowser(RouteConverter.getInstance().getFrame(), url);
    }

    public RoutingService getRoutingService() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getRoutingService();
    }

    public TravelMode getTravelMode() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getRoutingPreferencesModel().getTravelMode();
    }

    public boolean isAvoidFerries() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getRoutingPreferencesModel().isAvoidFerries();
    }

    public boolean isAvoidHighways() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getRoutingPreferencesModel().isAvoidHighways();
    }

    public boolean isAvoidTolls() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getRoutingPreferencesModel().isAvoidTolls();
    }

    public boolean isShowAllPositionsAfterLoading() {
        return RouteConverter.getInstance().getShowAllPositionsAfterLoading().getBoolean();
    }

    public boolean isRecenterAfterZooming() {
        return RouteConverter.getInstance().getRecenterAfterZooming().getBoolean();
    }

    public TileServerMapManager getTileServerMapManager() {
        return RouteConverter.getInstance().getTileServerMapManager();
    }

    public DistanceAndTimeAggregator getDistanceAndTimeAggregator() {
        return RouteConverter.getInstance().getDistanceAndTimeAggregator();
    }
}
