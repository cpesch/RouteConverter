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
package slash.navigation.maps.mapsforge.models;

import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.impl.TileMap;

import javax.swing.table.TableModel;

/**
 * Acts as a {@link TableModel} for {@link TileMap}s
 *
 * @author Christian Pesch
 */

public class TileMapTableModel extends ItemTableModel<TileMap> {
    public static final int DESCRIPTION_COLUMN = 0;
    public static final int ACTIVE_COLUMN = 1;

    public TileMapTableModel() {
        super(2);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == ACTIVE_COLUMN ? Boolean.class : String.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return columnIndex == ACTIVE_COLUMN ? getItem(rowIndex).isActive() : getItem(rowIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // the OpenStreetMap is always active
        return (columnIndex == ACTIVE_COLUMN && !(getItem(rowIndex) instanceof OpenStreetMap)) ||
                super.isCellEditable(rowIndex, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == ACTIVE_COLUMN) {
            TileMap map = getItem(rowIndex);
            map.setActive((Boolean) aValue);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }
}
