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

package slash.navigation.routes.impl;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Acts as a {@link TableModel} for the {@link RouteModel}s of a {@link CategoryTreeNode}.
 *
 * @author Christian Pesch
 */

public class RoutesTableModel extends AbstractTableModel {
    public static final int NAME_COLUMN = 0;
    public static final int CREATOR_COLUMN = 1;
    public static final int LENGTH_COLUMN = 2;
    public static final int DURATION_COLUMN = 3;
    public static final int COLUMN_COUNT = 4;

    private List<RouteModel> routes = new ArrayList<>();

    public void setRoutes(List<RouteModel> routes) {
        this.routes = routes;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return routes.size();
    }

    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getRoute(rowIndex);
    }

    public RouteModel getRoute(int rowIndex) {
        return routes.get(rowIndex);
    }

    public int getIndex(RouteModel route) {
        return routes.indexOf(route);
    }

    public void addRoute(RouteModel route) {
        if(!routes.add(route))
            throw new IllegalArgumentException("Route " + route + " not added to " + routes);

        int index = getIndex(route);
        if (index == -1)
            throw new IllegalArgumentException("Route " + route + " not found in " + routes);

        fireTableRowsInserted(index, index);
    }

    public void updateRoute(RouteModel route) {
        int index = getIndex(route);
        if (index == -1)
            throw new IllegalArgumentException("Route " + route + " not found in " + routes);
        fireTableRowsUpdated(index, index);
    }

    /**
     * Updates several routes with a single table event so that a {@link javax.swing.table.TableRowSorter}
     * with {@code setSortsOnUpdates} re-sorts once for the whole batch instead of once per route.
     * Routes that are no longer in the model are ignored.
     */
    public void updateRoutes(Collection<RouteModel> routesToUpdate) {
        int min = Integer.MAX_VALUE;
        int max = -1;
        for (RouteModel route : routesToUpdate) {
            int index = getIndex(route);
            if (index == -1)
                continue;
            min = Math.min(min, index);
            max = Math.max(max, index);
        }
        if (max >= 0)
            fireTableRowsUpdated(min, max);
    }

    public void deleteRoute(RouteModel route) {
        int index = getIndex(route);
        if (index == -1)
            throw new IllegalArgumentException("Route " + route + " not found in " + routes);

        if(!routes.remove(route))
            throw new IllegalArgumentException("Route " + route + " not deleted from " + routes);

        fireTableRowsDeleted(index, index);
    }
}
