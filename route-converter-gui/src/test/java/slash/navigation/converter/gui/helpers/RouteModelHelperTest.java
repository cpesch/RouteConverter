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
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
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
     * A routes table model whose rows can be reloaded like
     * {@link slash.navigation.routes.impl.RoutesTableModel#setRoutes}: column 0 holds the
     * row index (sortable), column 1 the {@link RouteModel}.
     */
    private static class ReloadableTableModel extends AbstractTableModel {
        private final RouteModel[] routeModels;
        private int rowCount;

        private ReloadableTableModel(RouteModel[] routeModels) {
            this.routeModels = routeModels;
            this.rowCount = routeModels.length;
        }

        private void reload(int rowCount) {
            this.rowCount = rowCount;
            fireTableDataChanged();
        }

        public int getRowCount() {
            return rowCount;
        }

        public int getColumnCount() {
            return 2;
        }

        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Integer.class : Object.class;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return columnIndex == 0 ? rowIndex : routeModels[rowIndex];
        }
    }

    private static RouteModel[] routeModels(int count) {
        RouteModel[] routeModels = new RouteModel[count];
        for (int i = 0; i < count; i++)
            routeModels[i] = new RouteModel(null, mock(Route.class));
        return routeModels;
    }

    private static JTable createTable(ReloadableTableModel model) {
        JTable table = new JTable(model);
        table.setRowSorter(new TableRowSorter<>(model));
        return table;
    }

    /**
     * Selects the given view row the way the table UI of a drag enabled table does on mouse
     * release: with the row that was pressed, even if the model was reloaded in between.
     */
    private static void selectPressedRow(JTable table, int pressedRow, boolean toggle) {
        table.changeSelection(pressedRow, 0, toggle, false);
    }

    @Test
    public void returnsTheSelectedRouteModel() {
        RouteModel[] routeModels = routeModels(3);
        JTable table = createTable(new ReloadableTableModel(routeModels));
        selectPressedRow(table, 2, false);

        assertEquals(List.of(routeModels[2]), getSelectedRouteModels(table));
        assertSame(routeModels[2], getSelectedRouteModel(table));
    }

    @Test
    public void skipsAPressedRowTheTableNoLongerHasAfterAReload() {
        RouteModel[] routeModels = routeModels(3);
        ReloadableTableModel model = new ReloadableTableModel(routeModels);
        JTable table = createTable(model);
        // like BrowsePanel, which opens the selected route from its selection listener
        List<RouteModel> selectedInListener = new ArrayList<>();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selectedInListener.add(getSelectedRouteModel(table));
        });

        model.reload(1);
        selectPressedRow(table, 2, false);

        assertEquals(1, selectedInListener.size());
        assertNull(selectedInListener.get(0));
        assertEquals(List.of(), getSelectedRouteModels(table));
        assertNull(getSelectedRouteModel(table));
    }

    @Test
    public void skipsAPressedRowTheSortedTableNoLongerHasAfterAReload() {
        RouteModel[] routeModels = routeModels(3);
        ReloadableTableModel model = new ReloadableTableModel(routeModels);
        JTable table = createTable(model);
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));

        model.reload(2);
        selectPressedRow(table, 2, false);

        assertEquals(List.of(), getSelectedRouteModels(table));
    }

    @Test
    public void convertsTheSelectedViewRowOfASortedTable() {
        RouteModel[] routeModels = routeModels(3);
        JTable table = createTable(new ReloadableTableModel(routeModels));
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        selectPressedRow(table, 0, false);

        assertEquals(List.of(routeModels[2]), getSelectedRouteModels(table));
    }

    @Test
    public void keepsTheStillValidSelectedRowAfterAReload() {
        RouteModel[] routeModels = routeModels(3);
        ReloadableTableModel model = new ReloadableTableModel(routeModels);
        JTable table = createTable(model);

        model.reload(1);
        selectPressedRow(table, 0, false);
        selectPressedRow(table, 2, true);

        assertEquals(List.of(routeModels[0]), getSelectedRouteModels(table));
    }

    @Test
    public void returnsEmptyListAndNullWhenNothingIsSelected() {
        JTable table = createTable(new ReloadableTableModel(routeModels(3)));

        assertEquals(List.of(), getSelectedRouteModels(table));
        assertNull(getSelectedRouteModel(table));
    }
}
