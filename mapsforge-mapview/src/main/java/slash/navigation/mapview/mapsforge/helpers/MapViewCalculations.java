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
package slash.navigation.mapview.mapsforge.helpers;

import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;
import java.util.List;

import static org.mapsforge.core.util.MercatorProjection.calculateGroundResolution;
import static org.mapsforge.core.util.MercatorProjection.getMapSize;

/**
 * Pure calculations behind the {@link slash.navigation.mapview.mapsforge.MapsforgeMapView} interaction
 * logic, factored out so they can be unit-tested without a live map view.
 *
 * @author Christian Pesch
 */

public class MapViewCalculations {
    private MapViewCalculations() {
    }

    /**
     * The ground distance (meters) that spans the given number of screen pixels at a zoom level -
     * used as the selection tolerance when hit-testing positions near a map click.
     */
    public static double thresholdForPixel(double latitude, byte zoomLevel, int tileSize, int selectionCirclePixels) {
        long mapSize = getMapSize(zoomLevel, tileSize);
        double metersPerPixel = calculateGroundResolution(latitude, mapSize);
        return metersPerPixel * selectionCirclePixels;
    }

    /**
     * The corner positions to fit into view when centering and zooming: the route's box if present,
     * plus the map's box when it is limited and does not already cover the route.
     */
    public static List<NavigationPosition> collectBoundingPositions(BoundingBox mapBoundingBox, BoundingBox routeBoundingBox) {
        List<NavigationPosition> positions = new ArrayList<>();

        // if there is a route and we center and zoom, then use the route bounding box
        if (routeBoundingBox != null) {
            positions.add(routeBoundingBox.northEast());
            positions.add(routeBoundingBox.southWest());
        }

        // if the map is limited
        if (mapBoundingBox != null) {
            // if there is a route
            if (routeBoundingBox != null) {
                positions.add(routeBoundingBox.northEast());
                positions.add(routeBoundingBox.southWest());
                // if the map is limited and doesn't cover the route
                if (!mapBoundingBox.contains(routeBoundingBox)) {
                    positions.add(mapBoundingBox.northEast());
                    positions.add(mapBoundingBox.southWest());
                }
                // if there just a map
            } else {
                positions.add(mapBoundingBox.northEast());
                positions.add(mapBoundingBox.southWest());
            }
        }

        return positions;
    }

    /**
     * The row a new position should be inserted at: after the last selected position, else after the
     * last position in the list, else at the start. Kept robust against a missing selection.
     */
    public static int computeAddRow(NavigationPosition lastSelected, PositionsModel positionsModel) {
        NavigationPosition position = lastSelected;
        // quite crude logic to be as robust as possible on failures
        if (position == null && positionsModel.getRowCount() > 0)
            position = positionsModel.getPosition(positionsModel.getRowCount() - 1);
        return position != null ? positionsModel.getIndex(position) + 1 : 0;
    }
}
