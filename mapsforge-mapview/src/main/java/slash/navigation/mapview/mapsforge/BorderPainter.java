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
package slash.navigation.mapview.mapsforge;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import slash.navigation.common.BoundingBox;
import slash.navigation.mapview.mapsforge.lines.Polyline;

import java.util.List;

import static java.util.Arrays.asList;
import static org.mapsforge.core.graphics.Color.BLUE;

/**
 * Draws the dashed blue map- and route-bounding-box borders as overlay {@link Polyline}s
 * for the {@link MapsforgeMapView}.
 *
 * @author Christian Pesch
 */

class BorderPainter {
    private final MapViewLayerOperations operations;
    private final GraphicFactory graphicFactory;
    private Polyline mapBorder, routeBorder;

    BorderPainter(MapViewLayerOperations operations, GraphicFactory graphicFactory) {
        this.operations = operations;
        this.graphicFactory = graphicFactory;
    }

    private List<LatLong> asLatLong(BoundingBox boundingBox) {
        return operations.asLatLong(asList(
                boundingBox.northEast(),
                boundingBox.getSouthEast(),
                boundingBox.southWest(),
                boundingBox.getNorthWest(),
                boundingBox.northEast()
        ));
    }

    private Polyline drawBorder(BoundingBox boundingBox) {
        Paint paint = graphicFactory.createPaint();
        paint.setColor(BLUE);
        paint.setStrokeWidth(3);
        paint.setDashPathEffect(new float[]{3, 12});
        Polyline polyline = new Polyline(asLatLong(boundingBox), paint, operations.getTileSize());
        operations.addLayer(polyline);
        return polyline;
    }

    public void showMapBorder(BoundingBox mapBoundingBox) {
        if (mapBorder != null) {
            operations.removeLayer(mapBorder);
            mapBorder = null;
        }
        if (routeBorder != null) {
            operations.removeLayer(routeBorder);
            routeBorder = null;
        }

        if (mapBoundingBox != null) {
            mapBorder = drawBorder(mapBoundingBox);

            BoundingBox routeBoundingBox = operations.getRouteBoundingBox();
            if (routeBoundingBox != null)
                routeBorder = drawBorder(routeBoundingBox);

            boolean zoomToMap = routeBoundingBox == null || mapBoundingBox.contains(routeBoundingBox);
            operations.centerAndZoom(mapBoundingBox, zoomToMap ? mapBoundingBox : routeBoundingBox, true, true);
        }
    }
}
