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
package slash.navigation.gui.models;

import org.junit.Test;

import javax.swing.table.DefaultTableModel;

import static org.junit.Assert.*;

/**
 * Tests for {@link FilteringTableModel}.
 *
 * @author Christian Pesch
 */
public class FilteringTableModelTest {

    private static FilterPredicate<String> including(final String... allowed) {
        return new FilterPredicate<>() {
            public String name() {
                return "allow " + String.join(",", allowed);
            }

            public boolean shouldInclude(String element) {
                for (String a : allowed)
                    if (a.equals(element))
                        return true;
                return false;
            }
        };
    }

    private static DefaultTableModel model(String... rows) {
        DefaultTableModel result = new DefaultTableModel(0, 1);
        for (String row : rows)
            result.addRow(new Object[]{row});
        return result;
    }

    @Test
    public void filtersRowsAndCountsOnlyIncluded() {
        FilteringTableModel<String> sut = new FilteringTableModel<>(model("a", "b", "c", "d"), including("b", "d"));

        assertEquals(2, sut.getRowCount());
        assertEquals("b", sut.getValueAt(0, 0));
        assertEquals("d", sut.getValueAt(1, 0));
    }

    @Test
    public void emptyWhenNothingMatches() {
        FilteringTableModel<String> sut = new FilteringTableModel<>(model("a", "b"), including("x"));

        assertEquals(0, sut.getRowCount());
    }

    @Test
    public void mapRowsTranslatesFilteredIndicesAndDropsUnknown() {
        FilteringTableModel<String> sut = new FilteringTableModel<>(model("a", "b", "c", "d"), including("b", "d"));

        // filtered rows 0,1 map to delegate rows 1,3; out-of-range row 5 is dropped
        assertArrayEquals(new int[]{1, 3}, sut.mapRows(new int[]{0, 1, 5}));
    }

    @Test
    public void setFilterPredicateReFiltersAndUpdatesCount() {
        FilteringTableModel<String> sut = new FilteringTableModel<>(model("a", "b", "c"), including("a"));
        assertEquals(1, sut.getRowCount());

        sut.setFilterPredicate(including("a", "c"));

        assertEquals(2, sut.getRowCount());
        assertEquals("a", sut.getValueAt(0, 0));
        assertEquals("c", sut.getValueAt(1, 0));
    }

    @Test
    public void reactsToDelegateChanges() {
        DefaultTableModel delegate = model("a", "b");
        FilteringTableModel<String> sut = new FilteringTableModel<>(delegate, including("a", "c"));
        assertEquals(1, sut.getRowCount());

        delegate.addRow(new Object[]{"c"});

        assertEquals(2, sut.getRowCount());
        assertEquals("c", sut.getValueAt(1, 0));
    }

    @Test
    public void writeOperationsAddressTheMappedDelegateRow() {
        DefaultTableModel delegate = model("a", "b", "c");
        FilteringTableModel<String> sut = new FilteringTableModel<>(delegate, including("c"));

        assertTrue(sut.isCellEditable(0, 0));
        sut.setValueAt("z", 0, 0);

        // filtered row 0 is delegate row 2
        assertEquals("z", delegate.getValueAt(2, 0));
    }

    @Test
    public void columnCountIsDeterminedByTheColumnModelNotHere() {
        FilteringTableModel<String> sut = new FilteringTableModel<>(model("a"), including("a"));

        assertThrows(IllegalArgumentException.class, sut::getColumnCount);
    }
}
