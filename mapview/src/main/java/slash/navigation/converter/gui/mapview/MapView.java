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

package slash.navigation.converter.gui.mapview;

import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.UnitSystemModel;

import java.awt.*;

/**
 * Interface for a component that displays the positions of a position list on a map.
 *
 * @author Christian Pesch
 */

public interface MapView extends PositionsSelectionModel {
    void initialize(PositionsModel positionsModel,
                    PositionsSelectionModel positionsSelectionModel,
                    CharacteristicsModel characteristicsModel,
                    MapViewCallback mapViewCallback,
                    boolean recenterAfterZooming,
                    boolean showCoordinates, boolean showWaypointDescription,
                    UnitSystemModel unitSystemModel);
    boolean isSupportedPlatform();
    boolean isInitialized();
    Throwable getInitializationCause();
    void dispose();

    Component getComponent();

    void resize();
    void setRecenterAfterZooming(boolean recenterAfterZooming);
    void setShowCoordinates(boolean showCoordinates);
    void setShowWaypointDescription(boolean showWaypointDescription);

    NavigationPosition getCenter();
    void print(String title, boolean withDirections);

    void addMapViewListener(MapViewListener listener);
    void removeMapViewListener(MapViewListener listener);

    void insertAllWaypoints(int[] startPositions);
    void insertOnlyTurnpoints(int[] startPositions);
}
