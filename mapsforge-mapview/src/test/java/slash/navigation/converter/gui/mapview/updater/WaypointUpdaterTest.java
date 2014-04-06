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
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WaypointUpdaterTest {
    private NavigationPosition p1 = new SimpleNavigationPosition(1.0, 0.0);
    private NavigationPosition p2 = new SimpleNavigationPosition(2.0, 0.0);
    private NavigationPosition p3 = new SimpleNavigationPosition(3.0, 0.0);
    private NavigationPosition p4 = new SimpleNavigationPosition(4.0, 0.0);

    private Waypoint w1 = new Waypoint(p1);
    private Waypoint w2 = new Waypoint(p2);
    private Waypoint w3 = new Waypoint(p3);
    private Waypoint w4 = new Waypoint(p4);


    @Test
    public void testInitiallyEmpty() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);

        assertTrue(waypointUpdater.getCurrentWaypoints().isEmpty());
        verify(waypointOperation, never()).add(new ArrayList<Waypoint>());
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());
    }

    @Test
    public void testAddOne() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 0);

        assertEquals(asList(w1), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());
    }

    @Test
    public void testAddTwo() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 1);

        assertEquals(asList(w1, w2), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());
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

        assertEquals(asList(w1, w2), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2));

        waypointUpdater.handleAdd(2, 2);

        assertEquals(asList(w1, w2, w3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w3));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());
    }

    @Test
    public void testRemove() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getPosition(3)).thenReturn(p4);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 3);

        assertEquals(asList(w1, w2, w3, w4), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2, w3, w4));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());

        waypointUpdater.handleRemove(1, 2);

        assertEquals(asList(w1, w4), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).remove(asList(w3, w2));
        verify(waypointOperation, never()).add(new ArrayList<Waypoint>());
    }

    @Test
    public void testRemoveAll() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        WaypointOperation waypointOperation = mock(WaypointOperation.class);

        WaypointUpdater waypointUpdater = new WaypointUpdater(positionsModel, waypointOperation);
        waypointUpdater.handleAdd(0, 2);

        assertEquals(asList(w1, w2, w3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2, w3));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());

        waypointUpdater.handleRemove(0, MAX_VALUE);

        assertEquals(new ArrayList<Waypoint>(), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, never()).add(new ArrayList<Waypoint>());
        verify(waypointOperation, times(1)).remove(asList(w3, w2, w1));
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

        assertEquals(asList(w1, w2, w3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2, w3));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());

        waypointUpdater.handleUpdate(1, 1);

        assertEquals(asList(w1, w2, w3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, never()).add(new ArrayList<Waypoint>());
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());
        verify(waypointOperation, times(1)).update(asList(w2));
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

        assertEquals(asList(w1, w2, w3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2, w3));
        verify(waypointOperation, never()).remove(new ArrayList<Waypoint>());

        waypointUpdater.handleUpdate(0, MAX_VALUE);

        assertEquals(asList(w1, w2, w3), waypointUpdater.getCurrentWaypoints());
        verify(waypointOperation, times(1)).add(asList(w1, w2, w3));
        verify(waypointOperation, never()).remove(asList(w1, w2, w3));
        verify(waypointOperation, times(1)).update(asList(w1, w2, w3));
    }
}
