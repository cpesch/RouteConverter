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
package slash.navigation.mapview.mapsforge.lines;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;

import static org.mapsforge.core.util.MercatorProjection.getMapSize;
import static org.mapsforge.core.util.MercatorProjection.latitudeToPixelY;
import static org.mapsforge.core.util.MercatorProjection.longitudeToPixelX;

/**
 * A line between two {@link LatLong}s on {@link MapsforgeMapView}
 *
 * @author Christian Pesch
 */

public class Line extends Layer {
    private final LatLong from;
    private final LatLong to;
    private final Paint paint;
    private final int tileSize;

    public Line(LatLong from, LatLong to, Paint paint, int tileSize) {
        this.to = to;
        this.from = from;
        this.paint = paint;
        this.tileSize = tileSize;
    }

    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        long mapSize = getMapSize(zoomLevel, tileSize);
        int fromX = (int) (longitudeToPixelX(from.longitude, mapSize) - topLeftPoint.x);
        int fromY = (int) (latitudeToPixelY(from.latitude, mapSize) - topLeftPoint.y);
        int toX = (int) (longitudeToPixelX(to.longitude, mapSize) - topLeftPoint.x);
        int toY = (int) (latitudeToPixelY(to.latitude, mapSize) - topLeftPoint.y);
        canvas.drawLine(fromX, fromY, toX, toY, paint);
    }
}
