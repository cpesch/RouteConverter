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

package slash.navigation.converter.gui.mapview;

import org.junit.Test;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TrackUpdaterTest {
    private NavigationPosition p1 = new Wgs84Position(1.0, 0.0, null, null, null, null);
    private NavigationPosition p2 = new Wgs84Position(2.0, 0.0, null, null, null, null);
    private NavigationPosition p3 = new Wgs84Position(3.0, 0.0, null, null, null, null);
    private NavigationPosition p4 = new Wgs84Position(4.0, 0.0, null, null, null, null);
    private PositionPair p1p2 = new PositionPair(p1, p2);
    private PositionPair p1p3 = new PositionPair(p1, p3);
    private PositionPair p1p4 = new PositionPair(p1, p4);
    private PositionPair p2p3 = new PositionPair(p2, p3);
    private PositionPair p3p2 = new PositionPair(p3, p2);
    private PositionPair p3p4 = new PositionPair(p3, p4);

    @Test
    public void testInitiallyEmpty() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);

        assertTrue(trackUpdater.getCurrentTrack().isEmpty());
        verify(trackOperation, never()).add(new ArrayList<PositionPair>());
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());
    }

    @Test
    public void testAddOne() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 0);

        assertTrue(trackUpdater.getCurrentTrack().isEmpty());
        verify(trackOperation, never()).add(new ArrayList<PositionPair>());
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());
    }

    @Test
    public void testAddTwo() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(asList(p1p2), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());
    }

    @Test
    public void testAddThree() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());
    }

    @Test
    public void testAppend() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(asList(p1p2), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2));

        trackUpdater.handleAdd(2, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p2p3));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());
    }

    @Test
    public void testInsert() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(asList(p1p2), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2));

        when(positionsModel.getPosition(1)).thenReturn(p3);
        when(positionsModel.getPosition(2)).thenReturn(p2);

        trackUpdater.handleAdd(1, 1);

        assertEquals(asList(p1p3, p3p2), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p3, p3p2));
        verify(trackOperation, times(1)).remove(asList(p1p2));
    }

    @Test
    public void testRemoveInTheMiddle() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getPosition(3)).thenReturn(p4);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 3);

        assertEquals(asList(p1p2, p2p3, p3p4), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3, p3p4));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleRemove(1, 2);

        assertEquals(asList(p1p4), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).remove(asList(p1p2, p2p3, p3p4));
        verify(trackOperation, times(1)).add(asList(p1p4));
    }

    @Test
    public void testRemoveFromStart() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleRemove(0, 0);

        assertEquals(asList(p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).remove(asList(p1p2));
        verify(trackOperation, never()).add(new ArrayList<PositionPair>());
    }

    @Test
    public void testRemoveUntilEnd() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleRemove(2, 2);

        assertEquals(asList(p1p2), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).remove(asList(p2p3));
        verify(trackOperation, never()).add(new ArrayList<PositionPair>());
    }

    @Test
    public void testRemoveSecondFromPair() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(asList(p1p2), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleRemove(1, 1);

        assertEquals(new ArrayList<PositionPair>(), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).remove(asList(p1p2));
        verify(trackOperation, never()).add(new ArrayList<PositionPair>());
    }

    @Test
    public void testUpdate() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleUpdate(1, 1);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(2)).add(asList(p1p2, p2p3));
        verify(trackOperation, times(1)).remove(asList(p1p2, p2p3));
    }

    @Test
    public void testUpdateAll() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleUpdate(0, MAX_VALUE);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(2)).add(asList(p1p2, p2p3));
        verify(trackOperation, times(1)).remove(asList(p1p2, p2p3));
    }
}
