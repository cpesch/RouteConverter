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
package slash.navigation.converter.gui.models;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.gui.models.FilterPredicate;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link FilteringPositionsModel}, which filters a delegate {@link PositionsModel}
 * and forwards the supported operations against the mapped delegate rows.
 *
 * @author Christian Pesch
 */
public class FilteringPositionsModelTest {
    private final NavigationPosition p0 = new SimpleNavigationPosition(0.0, 0.0);
    private final NavigationPosition p1 = new SimpleNavigationPosition(1.0, 0.0);
    private final NavigationPosition p2 = new SimpleNavigationPosition(2.0, 0.0);

    private PositionsModel delegate;
    private FilteringPositionsModel<NavigationPosition> sut;

    @Before
    public void setUp() {
        delegate = mock(PositionsModel.class);
        when(delegate.getRowCount()).thenReturn(3);
        when(delegate.getValueAt(0, 0)).thenReturn(p0);
        when(delegate.getValueAt(1, 0)).thenReturn(p1);
        when(delegate.getValueAt(2, 0)).thenReturn(p2);

        // include everything -> identity row mapping, so mapped row == delegate row
        FilterPredicate<NavigationPosition> includeAll = new FilterPredicate<>() {
            public String name() {
                return "all";
            }

            public boolean shouldInclude(NavigationPosition element) {
                return true;
            }
        };
        sut = new FilteringPositionsModel<>(delegate, includeAll);
    }

    @Test
    public void getRouteDelegates() {
        BaseRoute route = mock(BaseRoute.class);
        when(delegate.getRoute()).thenReturn(route);

        assertSame(route, sut.getRoute());
    }

    @Test
    public void setRouteDelegates() {
        BaseRoute route = mock(BaseRoute.class);

        sut.setRoute(route);

        verify(delegate).setRoute(route);
    }

    @Test
    public void getPositionMapsThroughToTheDelegateRow() {
        when(delegate.getPosition(1)).thenReturn(p1);

        assertSame(p1, sut.getPosition(1));
    }

    @Test
    public void getIndexMapsTheDelegateIndexBack() {
        when(delegate.getIndex(p1)).thenReturn(1);

        assertEquals(1, sut.getIndex(p1));
    }

    @Test
    public void editForwardsToTheMappedRow() {
        PositionColumnValues columnToValues = mock(PositionColumnValues.class);

        sut.edit(1, columnToValues, true, false);

        verify(delegate).edit(1, columnToValues, true, false);
    }

    @Test
    public void removeForwardsMappedRows() {
        sut.remove(new int[]{0, 2});

        verify(delegate).remove(aryEq(new int[]{0, 2}));
    }

    @Test
    public void continousRangeAndFullTableFlagsDelegate() {
        when(delegate.isContinousRangeOperation()).thenReturn(true);
        when(delegate.isFullTableModification()).thenReturn(true);

        assertTrue(sut.isContinousRangeOperation());
        assertTrue(sut.isFullTableModification());
    }

    @Test
    public void fireTableRowsUpdatedDelegates() {
        sut.fireTableRowsUpdated(0, 2, 1);

        verify(delegate).fireTableRowsUpdated(0, 2, 1);
    }

    @Test
    public void unsupportedStructuralOperationsThrow() {
        assertThrows(UnsupportedOperationException.class, () -> sut.getPositions(new int[]{0}));
        assertThrows(UnsupportedOperationException.class, () -> sut.sort(null));
        assertThrows(UnsupportedOperationException.class, () -> sut.revert());
        assertThrows(UnsupportedOperationException.class, () -> sut.top(new int[]{0}));
    }
}
