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

import static javax.swing.event.TableModelEvent.*;

/**
 * Converts {@link TileServer} to {@link LocalMap}
 *
 * @author Christian Pesch
 */

public class TileServerToTileMapMediator {
    private static final String THUNDER_FOREST_API_KEY = APIKeyRegistry.getInstance().getAPIKey("thunderforest", "map");
    private final ItemTableModel<TileServer> sourceModel;
    private final ItemTableModel<TileDownloadMap> destinationModel;
    private TableModelListener listener;

    public TileServerToTileMapMediator(ItemTableModel<TileServer> sourceModel, ItemTableModel<TileDownloadMap> destinationModel) {
        this.sourceModel = sourceModel;
        this.destinationModel = destinationModel;

        listener = e -> {
            switch (e.getType()) {
                case INSERT:
                    handleAdd(e.getFirstRow(), e.getLastRow());
                    break;
                case DELETE:
                    handleRemove(e.getFirstRow(), e.getLastRow());
                    break;
                case UPDATE:
                    handleUpdate(e.getFirstRow(), e.getLastRow());
                    break;
                default:
                    throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
            }
        };
        sourceModel.addTableModelListener(listener);
    }

    public void dispose() {
        sourceModel.removeTableModelListener(listener);
        listener = null;
    }

    private TileDownloadMap convert(TileServer tileServer) {
        TileServerMapSource tileSource = new TileServerMapSource(tileServer);
        if (THUNDER_FOREST_API_KEY != null && tileServer.getCopyright().toLowerCase().contains("thunderforest"))
            tileSource.setApiKey(THUNDER_FOREST_API_KEY);

        return new TileDownloadMap(tileServer.getId(), tileServer.getDescription(), tileServer.isActive(), tileSource, tileServer.getCopyrightText());
    }

    private void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileServer tileServer = sourceModel.getItem(i);
            TileDownloadMap map = convert(tileServer);
            destinationModel.add(i, map);
        }
    }

    private void handleUpdate(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileServer tileServer = sourceModel.getItem(i);
            TileDownloadMap map = convert(tileServer);
            destinationModel.update(i, map);
        }
    }

    private void handleRemove(int firstRow, int lastRow) {
        destinationModel.remove(firstRow, lastRow);
    }
}
