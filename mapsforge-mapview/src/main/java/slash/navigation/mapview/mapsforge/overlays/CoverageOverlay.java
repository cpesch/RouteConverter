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
package slash.navigation.mapview.mapsforge.overlays;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mapsforge.core.util.MercatorProjection.longitudeToPixelX;
import static org.mapsforge.core.util.MercatorProjection.latitudeToPixelY;
import static org.mapsforge.core.util.MercatorProjection.getMapSize;

/**
 * Paints a coverage overlay on the map canvas showing which parts of a map area
 * already have data downloaded (covered) and which don't (missing).
 *
 * @author Christian Pesch
 */
public class CoverageOverlay extends Layer {
    private final List<BoundingBox> coveredBoxes;
    private final List<BoundingBox> missingBoxes;
    private final Paint coveredPaint;
    private final Paint missingPaint;
    private final int tileSize;

    public CoverageOverlay(Map<BoundingBox, Boolean> coverageTiles, GraphicFactory graphicFactory, int tileSize) {
        this.coveredBoxes = new ArrayList<>();
        this.missingBoxes = new ArrayList<>();
        this.tileSize = tileSize;

        // Create covered paint (translucent green)
        this.coveredPaint = graphicFactory.createPaint();
        coveredPaint.setColor(graphicFactory.createColor(80, 0, 180, 0)); // alpha 80, RGB(0,180,0)
        coveredPaint.setStyle(Paint.Style.FILL);
        coveredPaint.setStrokeWidth(1);

        // Create missing paint (translucent red)
        this.missingPaint = graphicFactory.createPaint();
        missingPaint.setColor(graphicFactory.createColor(80, 200, 0, 0)); // alpha 80, RGB(200,0,0)
        missingPaint.setStyle(Paint.Style.FILL);
        missingPaint.setStrokeWidth(1);

        // Split tiles into covered and missing
        for (Map.Entry<BoundingBox, Boolean> entry : coverageTiles.entrySet()) {
            if (entry.getValue()) {
                coveredBoxes.add(entry.getKey());
            } else {
                missingBoxes.add(entry.getKey());
            }
        }
    }

    @Override
    public void draw(org.mapsforge.core.model.BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        // Draw missing tiles first (red), then covered tiles (green) on top
        for (BoundingBox box : missingBoxes) {
            drawBoundingBox(box, boundingBox, zoomLevel, canvas, topLeftPoint, missingPaint);
        }
        for (BoundingBox box : coveredBoxes) {
            drawBoundingBox(box, boundingBox, zoomLevel, canvas, topLeftPoint, coveredPaint);
        }
    }

    private void drawBoundingBox(BoundingBox boxToDraw, org.mapsforge.core.model.BoundingBox viewBoundingBox,
                                byte zoomLevel, Canvas canvas, Point topLeftPoint, Paint paint) {
        long mapSize = getMapSize(zoomLevel, tileSize);

        // Convert the custom BoundingBox to corners
        List<LatLong> corners = List.of(
            toLatLong(boxToDraw.northEast()),
            toLatLong(boxToDraw.getSouthEast()),
            toLatLong(boxToDraw.southWest()),
            toLatLong(boxToDraw.getNorthWest()),
            toLatLong(boxToDraw.northEast()) // Close the loop
        );

        // Convert each corner to screen coordinates
        List<Integer> xCoords = new ArrayList<>();
        List<Integer> yCoords = new ArrayList<>();

        for (LatLong corner : corners) {
            long x = longitudeToPixelX(corner.longitude, mapSize) - topLeftPoint.x;
            long y = latitudeToPixelY(corner.latitude, mapSize) - topLeftPoint.y;
            // Check if coordinates fit in int range
            if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE ||
                y < Integer.MIN_VALUE || y > Integer.MAX_VALUE) {
                return; // Skip drawing this box if coordinates overflow
            }
            xCoords.add((int) x);
            yCoords.add((int) y);
        }

        // Draw lines to form the rectangle
        for (int i = 0; i < xCoords.size() - 1; i++) {
            canvas.drawLine(xCoords.get(i), yCoords.get(i),
                          xCoords.get(i + 1), yCoords.get(i + 1), paint);
        }

        // Fill the rectangle by drawing horizontal lines
        int minY = yCoords.get(0); // north edge (smaller y in screen coordinates)
        int maxY = yCoords.get(1); // south edge (larger y in screen coordinates)
        int minX = xCoords.get(2); // west edge
        int maxX = xCoords.get(0); // east edge

        for (int y = minY; y <= maxY; y += 2) { // Step by 2 for performance
            canvas.drawLine(minX, y, maxX, y, paint);
        }
    }

    private LatLong toLatLong(slash.navigation.common.NavigationPosition position) {
        return new LatLong(position.getLatitude(), position.getLongitude());
    }
}