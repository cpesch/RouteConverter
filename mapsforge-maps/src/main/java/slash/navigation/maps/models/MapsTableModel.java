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
package slash.navigation.maps.models;

import slash.navigation.maps.Map;
import slash.navigation.maps.MapManager;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;

/**
 * Acts as a {@link TableModel} for the {@link Map}s of the {@link MapManager}.
 *
 * @author Christian Pesch
 */

public class MapsTableModel extends AbstractTableModel {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsTableModel.class);
    private List<Map> maps = new ArrayList<Map>();

    public List<Map> getMaps() {
        return maps;
    }

    public int getRowCount() {
        return maps.size();
    }

    public int getColumnCount() {
        return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getMap(rowIndex);
    }

    public Map getMap(int rowIndex) {
        return maps.get(rowIndex);
    }

    private void addMap(Map map) {
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

    void updateMap(Map map) {
        final int index = maps.indexOf(map);
        if (index == -1)
            throw new IllegalArgumentException("Map " + map + " not found in " + maps);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsUpdated(index, index);
            }
        });
    }

    public void addOrUpdateMap(Map map) {
        int index = maps.indexOf(map);
        if (index == -1)
            addMap(map);
        else
            updateMap(map);
    }

    private void removeMap(Map map) {
        final int index = maps.indexOf(map);
        if (index == -1)
            throw new IllegalArgumentException("Map " + map + " not found in " + maps);

        if (!maps.remove(map))
            throw new IllegalArgumentException("Map " + map + " not removed from " + maps);

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableRowsDeleted(index, index);
            }
        });
    }

    public void clear() {
        this.maps = new ArrayList<Map>();

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                fireTableDataChanged();
            }
        });
    }
}