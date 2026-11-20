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
package slash.navigation.maps.mapsforge.helpers;

import slash.common.helpers.APIKeyRegistry;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.impl.TileDownloadMap;
import slash.navigation.maps.mapsforge.models.TileServerMapSource;
import slash.navigation.maps.tileserver.TileServer;

import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;

import static java.util.Locale.ROOT;
import static javax.swing.event.TableModelEvent.*;

/**
 * Converts {@link TileServer} to {@link LocalMap}
 *
 * @author Christian Pesch
 */

public class TileServerToTileMapMediator {
    private static final String THUNDER_FOREST_API_KEY = APIKeyRegistry.getInstance().getAPIKey("thunderforest", "map");
    private static final String MAPBOX_API_KEY = APIKeyRegistry.getInstance().getAPIKey("mapbox", "map");
    private final ItemTableModel<TileServer> sourceModel;
    private final ItemTableModel<TileDownloadMap> destinationModel;
    // index-aligned with the source model, which is append-only and unsorted; the destination model may be
    // sorted and may hold rows the source does not, so its row indices must never be derived from source rows
    private final List<TileDownloadMap> converted = new ArrayList<>();
    private TableModelListener listener;

    public TileServerToTileMapMediator(ItemTableModel<TileServer> sourceModel, ItemTableModel<TileDownloadMap> destinationModel) {
        this.sourceModel = sourceModel;
        this.destinationModel = destinationModel;

        listener = e -> {
            switch (e.getType()) {
                case INSERT -> handleAdd(e.getFirstRow(), e.getLastRow());
                case DELETE -> handleRemove(e.getFirstRow(), e.getLastRow());
                case UPDATE -> handleUpdate(e.getFirstRow(), e.getLastRow());
                default -> throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
            }
        };
        sourceModel.addTableModelListener(listener);
        handleAdd(0, sourceModel.getRowCount() - 1);
    }

    public void dispose() {
        sourceModel.removeTableModelListener(listener);
        listener = null;
    }

    static TileDownloadMap convert(TileServer tileServer) {
        TileServerMapSource tileSource = new TileServerMapSource(tileServer);
        if (THUNDER_FOREST_API_KEY != null && tileServer.copyright().toLowerCase(ROOT).contains("thunderforest"))
            tileSource.setApiKey(THUNDER_FOREST_API_KEY);
        if (MAPBOX_API_KEY != null && tileServer.copyright().toLowerCase(ROOT).contains("mapbox"))
            tileSource.setApiKey(MAPBOX_API_KEY);
        if (tileServer.copyright().toLowerCase(ROOT).contains("openstreetmap"))
            tileSource.setReferer("https://www.routeconverter.com");

        // description is the human-readable name shown and sorted by, url is the stable id used as preference key
        return new TileDownloadMap(tileServer.description(), tileServer.id(), tileServer.active(), tileSource, tileServer.copyrightText());
    }

    private void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileDownloadMap map = convert(sourceModel.getItem(i));
            converted.add(i, map);
            // the destination's comparator decides the position
            destinationModel.addOrUpdateItem(map);
        }
    }

    private void handleUpdate(int firstRow, int lastRow) {
        int last = Math.min(lastRow, Math.min(sourceModel.getRowCount(), converted.size()) - 1);
        for (int i = firstRow; i <= last; i++) {
            TileDownloadMap old = converted.get(i);
            TileDownloadMap map = convert(sourceModel.getItem(i));
            converted.set(i, map);
            // source updates re-add an equal record, so the sort key cannot change: keep the row in place
            int index = destinationModel.getIndex(old);
            if (index != -1)
                destinationModel.update(index, map);
        }
    }

    private void handleRemove(int firstRow, int lastRow) {
        for (int i = Math.min(lastRow, converted.size() - 1); i >= firstRow; i--) {
            TileDownloadMap old = converted.remove(i);
            if (destinationModel.contains(old))
                destinationModel.removeItem(old);
        }
    }
}
