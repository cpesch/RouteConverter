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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.catalog.model;

import slash.navigation.catalog.domain.Route;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Acts as a {@link TableModel} for the routes of a {@link CategoryTreeNode}.
 *
 * @author Christian Pesch
 */

public class RoutesListModel extends AbstractTableModel {
    private List<Route> routes = new ArrayList<Route>();

    public RoutesListModel(List<Route> routes) {
        setRoutes(routes);
    }

    public RoutesListModel() {
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return routes.size();
    }

    public int getColumnCount() {
        return 3;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getRoute(rowIndex);
    }

    public Route getRoute(int rowIndex) {
        return routes.get(rowIndex);
    }

    public void addRoute(Route route) {
        routes.add(route);
        int index = routes.indexOf(route);
        fireTableRowsInserted(index, index);
    }

    public void updateRoute(Route route) {
        int index = routes.indexOf(route);
        fireTableRowsUpdated(index, index);
    }

    public void deleteRoute(Route route) {
        int index = routes.indexOf(route);
        routes.remove(route);
        fireTableRowsDeleted(index, index);
    }
}
