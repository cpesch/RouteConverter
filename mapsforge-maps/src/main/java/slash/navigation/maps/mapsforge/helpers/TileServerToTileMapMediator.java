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

import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.impl.TileMap;
import slash.navigation.maps.mapsforge.models.TileServerMapSource;
import slash.navigation.maps.tileserver.TileServer;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import static javax.swing.event.TableModelEvent.DELETE;
import static javax.swing.event.TableModelEvent.INSERT;
import static javax.swing.event.TableModelEvent.UPDATE;

/**
 * Converts {@link TileServer} to {@link LocalMap}
 *
 * @author Christian Pesch
 */

public class TileServerToTileMapMediator {
    private final ItemTableModel<TileServer> sourceModel;
    private final ItemTableModel<TileMap> destinationModel;
    private TableModelListener listener;

    public TileServerToTileMapMediator(ItemTableModel<TileServer> sourceModel, ItemTableModel<TileMap> destinationModel) {
        this.sourceModel = sourceModel;
        this.destinationModel = destinationModel;

        listener = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
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
            }
        };
        sourceModel.addTableModelListener(listener);
    }

    public void dispose() {
        sourceModel.removeTableModelListener(listener);
        listener = null;
    }

    private TileMap convert(TileServer tileServer) {
        return new TileMap(tileServer.getId(), tileServer.getDescription(), tileServer.isActive(), new TileServerMapSource(tileServer));
    }

    private void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileServer tileServer = sourceModel.getItem(i);
            TileMap tileMap = convert(tileServer);
            destinationModel.add(i, tileMap);
        }
    }

    private void handleUpdate(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileServer tileServer = sourceModel.getItem(i);
            TileMap tileMap = convert(tileServer);
            destinationModel.update(i, tileMap);
        }
    }

    private void handleRemove(int firstRow, int lastRow) {
        destinationModel.remove(firstRow, lastRow);
    }
}
