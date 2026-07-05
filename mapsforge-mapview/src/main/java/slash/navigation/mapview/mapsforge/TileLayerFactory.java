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

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.tileserver.TileServer;

import java.io.File;

/**
 * Creates the concrete mapsforge tile layers ({@link org.mapsforge.map.layer.renderer.TileRendererLayer},
 * MBTiles, download and overlay layers with their tile caches) for the {@link MapsforgeMapView}.
 * <p>
 * Kept behind an interface so the layer-management orchestration (map switch, overlays, background)
 * can be tested against a mock while this real construction - which needs map files, tile caches and
 * network sources - stays isolated here.
 *
 * @author Christian Pesch
 */

interface TileLayerFactory {
    Layer createLayerForMap(LocalMap map);

    TileDownloadLayer createOverlayLayer(TileServer tileServer);

    Layer createBackgroundLayer(File backgroundMap);
}
