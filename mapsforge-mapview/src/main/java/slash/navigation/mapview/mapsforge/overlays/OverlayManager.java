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

import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.model.DisplayModel;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.tileserver.TileServer;
import slash.navigation.mapview.mapsforge.tiles.TileLayerFactory;

/**
 * Manages the tile-server overlay download layers for the {@link MapsforgeMapView}: keeps them on
 * their own {@link GroupLayer} and adds/removes them as the applied-overlays model changes. The tile
 * layer construction is delegated to a {@link TileLayerFactory} and the view interactions go through
 * {@link Context}, so this orchestration can be unit-tested against mocks.
 *
 * @author Christian Pesch
 */

public class OverlayManager {
    /**
     * The {@link MapsforgeMapView} interactions the overlay handling needs.
     */
    public interface Context {
        DisplayModel getDisplayModel();

        void redrawLayers();

        /**
         * Nudge the map so a freshly added overlay is drawn immediately.
         */
        void forceOverlayDisplay();
    }

    private final GroupLayer overlaysLayer = new GroupLayer();
    private final TileLayerFactory tileLayerFactory;
    private final ItemTableModel<TileServer> appliedOverlaysModel;
    private final Context context;

    public OverlayManager(TileLayerFactory tileLayerFactory, ItemTableModel<TileServer> appliedOverlaysModel, Context context) {
        this.tileLayerFactory = tileLayerFactory;
        this.appliedOverlaysModel = appliedOverlaysModel;
        this.context = context;
    }

    public GroupLayer getLayer() {
        return overlaysLayer;
    }

    public void insert(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileServer tileServer = appliedOverlaysModel.getItem(i);
            TileDownloadLayer overlay = tileLayerFactory.createOverlayLayer(tileServer);
            overlaysLayer.layers.add(overlay);
            overlay.setDisplayModel(context.getDisplayModel());
            overlay.start();
            context.redrawLayers();
        }
        // force immediate display of the overlay
        context.forceOverlayDisplay();
    }

    public void delete(int firstRow, int lastRow) {
        for (int i = lastRow; i >= firstRow; i--) {
            if (i >= overlaysLayer.layers.size())
                continue;

            Layer layer = overlaysLayer.layers.get(i);
            TileDownloadLayer overlay = (TileDownloadLayer) layer;
            overlaysLayer.layers.remove(overlay);
            overlaysLayer.requestRedraw();
            overlay.onDestroy();
        }
    }
}
