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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.routes.Route;
import slash.navigation.routes.impl.RouteModel;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.getSelectedRouteModel;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.getSelectedRouteModels;

/**
 * Tests for {@link RouteModelHelper}.
 *
 * @author Christian Pesch
 */
public class RouteModelHelperTest {

    /**
     * A table model whose row count can be shrunk without firing a table event -
     * reproduces a routes table whose underlying model reloaded while the view
     * (selection, row sorter) still refers to the old, larger row count.
     */
    private static class ShrinkableTableModel extends AbstractTableModel {
        private final RouteModel[] routeModels;
        private int rowCount;

        private ShrinkableTableModel(RouteModel[] routeModels) {
            this.routeModels = routeModels;
            this.rowCount = routeModels.length;
        }

        private void shrinkRowCountWithoutEvent(int rowCount) {
            this.rowCount = rowCount;
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return columnIndex == 1 ? routeModels[rowIndex] : null;
        }
    }

    private static RouteModel[] routeModels(int count) {
        RouteModel[] routeModels = new RouteModel[count];
        for (int i = 0; i < count; i++)
            routeModels[i] = new RouteModel(null, mock(Route.class));
        return routeModels;
    }

    private static JTable createTable(ShrinkableTableModel model) {
        JTable table = new JTable(model);
        table.setRowSorter(new TableRowSorter<>(model));
        return table;
    }

    @Test
    public void returnsTheSelectedRouteModel() {
        RouteModel[] routeModels = routeModels(3);
        JTable table = createTable(new ShrinkableTableModel(routeModels));
        table.getSelectionModel().setSelectionInterval(2, 2);

        assertEquals(List.of(routeModels[2]), getSelectedRouteModels(table));
        assertSame(routeModels[2], getSelectedRouteModel(table));
    }

    @Test
    public void skipsASelectedRowTheModelNoLongerHasAfterASilentReload() {
        RouteModel[] routeModels = routeModels(3);
        ShrinkableTableModel model = new ShrinkableTableModel(routeModels);
        JTable table = createTable(model);
        table.getSelectionModel().setSelectionInterval(2, 2);

        model.shrinkRowCountWithoutEvent(1);

        assertEquals(List.of(), getSelectedRouteModels(table));
        assertNull(getSelectedRouteModel(table));
    }

    @Test
    public void keepsTheStillValidSelectedRowAfterASilentReload() {
        RouteModel[] routeModels = routeModels(3);
        ShrinkableTableModel model = new ShrinkableTableModel(routeModels);
        JTable table = createTable(model);
        table.getSelectionModel().addSelectionInterval(0, 0);
        table.getSelectionModel().addSelectionInterval(2, 2);

        model.shrinkRowCountWithoutEvent(1);

        assertEquals(List.of(routeModels[0]), getSelectedRouteModels(table));
    }

    @Test
    public void returnsEmptyListAndNullWhenNothingIsSelected() {
        JTable table = createTable(new ShrinkableTableModel(routeModels(3)));

        assertEquals(List.of(), getSelectedRouteModels(table));
        assertNull(getSelectedRouteModel(table));
    }
}
