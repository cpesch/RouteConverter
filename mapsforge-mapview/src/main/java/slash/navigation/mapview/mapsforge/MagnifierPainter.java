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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Marker;
import slash.navigation.common.NavigationPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Draws magnifier icons over a set of {@link NavigationPosition}s as overlay {@link Marker}s
 * for the {@link MapsforgeMapView}.
 *
 * @author Christian Pesch
 */

class MagnifierPainter {
    private static final Logger log = Logger.getLogger(MagnifierPainter.class.getName());

    private final MapViewLayerOperations operations;
    private final List<Layer> markers = new ArrayList<>();
    private Bitmap magnifierIcon;

    MagnifierPainter(MapViewLayerOperations operations, GraphicFactory graphicFactory) {
        this.operations = operations;
        try {
            magnifierIcon = graphicFactory.renderSvg(MapsforgeMapView.class.getResourceAsStream("magnifier.svg"),
                    1.0f, 57, 56, 100, 1234567891);
        } catch (IOException e) {
            log.severe("Cannot create magnifier icon: " + e);
        }
    }

    public void showPositionMagnifier(List<NavigationPosition> positions) {
        if (!markers.isEmpty()) {
            operations.removeLayers(markers);
            markers.clear();
        }

        if (positions != null && !positions.isEmpty()) {
            List<Layer> icons = positions.stream()
                    .map(position -> new Marker(operations.asLatLong(position), magnifierIcon, -10, 13))
                    .collect(Collectors.toList());
            operations.addLayers(icons);
            markers.addAll(icons);
        }
    }
}
