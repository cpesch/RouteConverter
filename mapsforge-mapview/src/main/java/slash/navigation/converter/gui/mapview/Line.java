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

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;

import static org.mapsforge.core.graphics.Color.BLUE;
import static org.mapsforge.core.util.MercatorProjection.latitudeToPixelY;
import static org.mapsforge.core.util.MercatorProjection.longitudeToPixelX;
import static slash.navigation.converter.gui.mapview.AwtGraphicMapView.GRAPHIC_FACTORY;

/**
 * A line between two {@link LatLong}s on {@link MapsforgeMapView}
 *
 * @author Christian Pesch
 */

public class Line extends Layer {
    private static final Paint paint;
    static {
        paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor(BLUE);
        paint.setStrokeWidth(3);
    }
    private final LatLong from;
    private final LatLong to;

    public Line(LatLong from, LatLong to) {
        this.to = to;
        this.from = from;
    }

    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        int fromX = (int) (longitudeToPixelX(from.longitude, zoomLevel) - topLeftPoint.x);
        int fromY = (int) (latitudeToPixelY(from.latitude, zoomLevel) - topLeftPoint.y);
        int toX = (int) (longitudeToPixelX(to.longitude, zoomLevel) - topLeftPoint.x);
        int toY = (int) (latitudeToPixelY(to.latitude, zoomLevel) - topLeftPoint.y);
        canvas.drawLine(fromX, fromY, toX, toY, paint);
    }
}
