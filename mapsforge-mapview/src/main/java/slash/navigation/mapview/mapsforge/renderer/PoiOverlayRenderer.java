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
package slash.navigation.mapview.mapsforge.renderer;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.DisplayModel;
import slash.navigation.common.NavigationPosition;
import slash.navigation.mapview.PoiMarker;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * Renders the POIs of the POI overlay as colored markers for the {@link MapsforgeMapView}, on a
 * dedicated {@link GroupLayer} the map view keeps directly above the map, and finds the POI under
 * the mouse for hovering and inserting.
 *
 * @author Christian Pesch
 */

public class PoiOverlayRenderer {
    private static final String OPACITY = "0.9";

    private final MapsforgeMapView mapView;
    private final GroupLayer layer = new GroupLayer();
    private final Map<Color, Bitmap> icons = new HashMap<>();
    private volatile List<PoiMarker> markers = emptyList();

    public PoiOverlayRenderer(MapsforgeMapView mapView) {
        this.mapView = mapView;
    }

    public GroupLayer getLayer() {
        return layer;
    }

    public void show(List<PoiMarker> markers) {
        List<PoiMarker> shown = markers != null ? List.copyOf(markers) : List.of();
        DisplayModel displayModel = mapView.getDisplayModel();
        // GroupLayer.draw() iterates the layers under synchronized(this) on the render thread
        synchronized (layer) {
            layer.layers.clear();
            for (PoiMarker marker : shown) {
                NavigationPosition position = marker.position();
                if (position == null || !position.hasCoordinates())
                    continue;
                Marker layerMarker = new Marker(mapView.asLatLong(position), getIcon(marker.color()), 0, 0);
                layerMarker.setDisplayModel(displayModel);
                layer.layers.add(layerMarker);
            }
        }
        this.markers = shown;
        layer.requestRedraw();
    }

    public PoiMarker getMarkerAt(Function<NavigationPosition, Point> toPixels, Point point, double radius) {
        return findNearest(markers, toPixels, point, radius);
    }

    static PoiMarker findNearest(List<PoiMarker> markers, Function<NavigationPosition, Point> toPixels, Point point, double radius) {
        if (point == null)
            return null;
        PoiMarker nearest = null;
        double nearestDistance = radius;
        for (PoiMarker marker : markers) {
            if (marker.position() == null || !marker.position().hasCoordinates())
                continue;
            Point pixels = toPixels.apply(marker.position());
            if (pixels == null)
                continue;
            double distance = pixels.distance(point);
            if (distance <= nearestDistance) {
                nearest = marker;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private synchronized Bitmap getIcon(Color color) {
        return icons.computeIfAbsent(color, c ->
                mapView.createWaypointIcon(format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()), OPACITY));
    }
}
