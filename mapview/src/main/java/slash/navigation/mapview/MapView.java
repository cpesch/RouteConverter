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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.BooleanModel;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.converter.gui.models.FixMapModeModel;
import slash.navigation.converter.gui.models.GoogleMapsServerModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.UnitSystemModel;

import java.awt.*;
import java.util.List;

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
                    BooleanModel showAllPositionsAfterLoading,
                    BooleanModel recenterAfterZooming,
                    BooleanModel showCoordinates,
                    BooleanModel showWaypointDescription,
                    FixMapModeModel fixMapModeModel,
                    ColorModel routeColorModel,
                    ColorModel trackColorModel,
                    UnitSystemModel unitSystemModel,
                    GoogleMapsServerModel googleMapsServerModel);
    boolean isInitialized();
    boolean isDownload();
    boolean isSupportsPrinting();
    boolean isSupportsPrintingWithDirections();

    Throwable getInitializationCause();
    void dispose();

    Component getComponent();

    void resize();
    void showAllPositions();
    void showMapBorder(BoundingBox mapBoundingBox);

    NavigationPosition getCenter();

    void setSelectedPositions(List<NavigationPosition> selectedPositions);
    void print(String title, boolean withDirections);

    void addMapViewListener(MapViewListener listener);
    void removeMapViewListener(MapViewListener listener);
}
