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
import slash.navigation.converter.gui.models.*;
import slash.navigation.gui.models.BooleanModel;

import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Interface for a component that displays the positions of a position list on a map.
 *
 * @author Christian Pesch
 */

public interface MapView extends PositionsSelectionModel {
    void initialize(PositionsModel positionsModel,
                    MapPreferencesModel preferencesModel,
                    MapViewCallback mapViewCallback);
    boolean isInitialized();
    boolean isDownload();
    String getMapIdentifier();
    String getMapsPath(); // only MapsforgeMapView
    void setMapsPath(String path) throws IOException;
    String getThemesPath(); // only MapsforgeMapView
    void setThemesPath(String path) throws IOException;

    Throwable getInitializationCause();
    void dispose();

    Component getComponent();

    void resize(); // only BrowserMapView
    void showAllPositions();
    void showMapBorder(BoundingBox mapBoundingBox); // only MapsforgeMapView
    void showPositionMagnifier(List<NavigationPosition> positions);

    NavigationPosition getCenter();

    void setSelectedPositions(List<NavigationPosition> selectedPositions);
    boolean isSupportsPrinting();
    void print(String title);
}
