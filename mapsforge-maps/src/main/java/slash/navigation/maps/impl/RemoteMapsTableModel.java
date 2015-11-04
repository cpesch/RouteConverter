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
package slash.navigation.maps.impl;

import slash.navigation.maps.MapManager;
import slash.navigation.maps.RemoteMap;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;

/**
 * Acts as a {@link TableModel} for the available {@link RemoteMap}s of the {@link MapManager}.
 *
 * @author Christian Pesch
 */

public class RemoteMapsTableModel extends AbstractTableModel {
    public static final int DATASOURCE_COLUMN = 0;
    public static final int DESCRIPTION_COLUMN = 1;
    public static final int SIZE_COLUMN = 2;

    private List<RemoteMap> maps = new ArrayList<>();

    public List<RemoteMap> getMaps() {
        return maps;
    }

    public void setMaps(List<RemoteMap> maps) {
        this.maps = maps;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return maps.size();
    }

    public int getColumnCount() {
        return SIZE_COLUMN + 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getMap(rowIndex);
    }

    public RemoteMap getMap(int rowIndex) {
        return maps.get(rowIndex);
    }

    private void addMap(RemoteMap map) {
        if (!maps.add(map))
            throw new IllegalArgumentException("Map " + map + " not added to " + maps);

        final int index = maps.indexOf(map);
        if (index == -1)
            throw new IllegalArgumentException("Map " + map + " not found in " + maps);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsInserted(index, index);
            }
        });
    }

    void updateMap(RemoteMap map) {
        final int index = maps.indexOf(map);
        if (index == -1)
            throw new IllegalArgumentException("Map " + map + " not found in " + maps);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsUpdated(index, index);
            }
        });
    }

    public void addOrUpdateMap(RemoteMap map) {
        int index = maps.indexOf(map);
        if (index == -1)
            addMap(map);
        else
            updateMap(map);
    }
}