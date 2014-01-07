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

package slash.navigation.converter.gui.mapview.updater;

import org.junit.Test;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WaypointUpdaterTest {
    private NavigationPosition p1 = new Wgs84Position(1.0, 0.0, null, null, null, null);
    private NavigationPosition p2 = new Wgs84Position(2.0, 0.0, null, null, null, null);
    private NavigationPosition p3 = new Wgs84Position(3.0, 0.0, null, null, null, null);
    private NavigationPosition p4 = new Wgs84Position(4.0, 0.0, null, null, null, null);

    @Test
    public void testInitiallyEmpty() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);

        assertTrue(waypointUpdater.getCurrentWaypoints().isEmpty());
        verify(waypointOperation, never()).add(new ArrayList<NavigationPosition>());
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());
    }

    @Test
    public void testAddOne() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 0);

        assertEquals(asList(p1), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p1));
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());
    }

    @Test
    public void testAddTwo() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 1);

        assertEquals(asList(p1, p2), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p1, p2));
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());
    }

    @Test
    public void testAppend() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 1);

        assertEquals(asList(p1, p2), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p1, p2));

        waypointUpdater.handleAdd(2, 2);

        assertEquals(asList(p1, p2, p3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p3));
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());
    }

    @Test
    public void testRemove() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 2);

        assertEquals(asList(p1, p2, p3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p1, p2, p3));
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());

        waypointUpdater.handleRemove(1, 1);

        assertEquals(asList(p1, p3), waypointUpdater.getCurrentWaypoints());
       verify(waypointOperation, times(1)).remove(asList(p2));
        verify(waypointOperation, never()).add(new ArrayList<NavigationPosition>());
    }

    @Test
    public void testUpdate() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 2);

        assertEquals(asList(p1, p2, p3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p1, p2, p3));
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());

        waypointUpdater.handleUpdate(1, 1);

        assertEquals(asList(p1, p2, p3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p2));
        verify(waypointOperation, times(1)).remove(asList(p2));
    }

    @Test
    public void testUpdateAll() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 2);

        assertEquals(asList(p1, p2, p3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(p1, p2, p3));
        verify(waypointOperation, never()).remove(new ArrayList<NavigationPosition>());

        waypointUpdater.handleUpdate(0, MAX_VALUE);

        assertEquals(asList(p1, p2, p3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(2)).add(asList(p1, p2, p3));
        verify(waypointOperation, times(1)).remove(asList(p1, p2, p3));
    }
}
