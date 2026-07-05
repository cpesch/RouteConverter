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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;

import java.util.List;

/**
 * The narrow set of {@link MapsforgeMapView} operations that extracted overlay painters
 * (border, magnifier) need: coordinate conversion, layer add/remove and centering. Kept
 * as an interface so those painters can be unit-tested against a mock instead of a live map.
 *
 * @author Christian Pesch
 */

interface MapViewLayerOperations {
    int getTileSize();

    LatLong asLatLong(NavigationPosition position);

    List<LatLong> asLatLong(List<NavigationPosition> positions);

    void addLayer(Layer layer);

    void addLayers(List<Layer> layers);

    void removeLayer(Layer layer);

    void removeLayers(List<Layer> layers);

    BoundingBox getRouteBoundingBox();

    void centerAndZoom(BoundingBox mapBoundingBox, BoundingBox routeBoundingBox, boolean alwaysZoom, boolean alwaysRecenter);
}
