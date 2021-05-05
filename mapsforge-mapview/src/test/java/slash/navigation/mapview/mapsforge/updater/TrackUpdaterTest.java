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

package slash.navigation.mapview.mapsforge.updater;

import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TrackUpdaterTest {
    private NavigationPosition p1 = new SimpleNavigationPosition(1.0, 0.0);
    private NavigationPosition p2 = new SimpleNavigationPosition(2.0, 0.0);
    private NavigationPosition p3 = new SimpleNavigationPosition(3.0, 0.0);
    private NavigationPosition p4 = new SimpleNavigationPosition(4.0, 0.0);
    private PairWithLayer p1p2 = new PairWithLayer(p1, p2, 0);
    private PairWithLayer p1p3 = new PairWithLayer(p1, p3, 1);
    private PairWithLayer p1p4 = new PairWithLayer(p1, p4, 2);
    private PairWithLayer p2p3 = new PairWithLayer(p2, p3, 3);
    private PairWithLayer p2p4 = new PairWithLayer(p2, p4, 4);
    private PairWithLayer p3p1 = new PairWithLayer(p3, p1, 5);
    private PairWithLayer p3p4 = new PairWithLayer(p3, p4, 6);

    @Test
    public void testInitiallyEmpty() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);

        assertTrue(trackUpdater.getPairWithLayers().isEmpty());
        verify(trackOperation, never()).add(new ArrayList<>());
        verify(trackOperation, never()).remove(new ArrayList<>());
    }

    @Test
    public void testAddOne() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getRowCount()).thenReturn(1);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 0);

        assertTrue(trackUpdater.getPairWithLayers().isEmpty());
        verify(trackOperation, never()).add(new ArrayList<>());
        verify(trackOperation, never()).remove(new ArrayList<>());
    }

    @Test
    public void testAddTwo() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(singletonList(p1p2), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        verify(trackOperation, times(1)).add(singletonList(p1p2));
        verify(trackOperation, never()).remove(new ArrayList<>());
    }

    @Test
    public void testAddThree() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());
    }

    @Test
    public void testAppend() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(singletonList(p1p2), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        verify(trackOperation, times(1)).add(singletonList(p1p2));

        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);

        trackUpdater.handleAdd(2, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(singletonList(p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());
    }

    @Test
    public void testInsert() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(singletonList(p1p2), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        verify(trackOperation, times(1)).add(singletonList(p1p2));

        // prepend
        when(positionsModel.getPosition(0)).thenReturn(p3);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getPosition(2)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(3);

        trackUpdater.handleAdd(0, 0);

        assertEquals(asList(p3p1, p1p2), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(0, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(singletonList(p3p1));
        verify(trackOperation, never()).remove(new ArrayList<>());

        // append
        when(positionsModel.getPosition(3)).thenReturn(p4);
        when(positionsModel.getRowCount()).thenReturn(4);

        trackUpdater.handleAdd(3, 3);

        assertEquals(asList(p3p1, p1p2, p2p4), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(0, trackUpdater.getPairWithLayers().get(1).getRow());
        assertEquals(2, trackUpdater.getPairWithLayers().get(2).getRow());
        verify(trackOperation, times(1)).add(singletonList(p2p4));
        verify(trackOperation, never()).remove(new ArrayList<>());
    }

    @Test
    public void testRemoveOneInTheMiddle() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getPosition(3)).thenReturn(p4);
        when(positionsModel.getRowCount()).thenReturn(4);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 3);

        assertEquals(asList(p1p2, p2p3, p3p4), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        assertEquals(2, trackUpdater.getPairWithLayers().get(2).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3, p3p4));
        verify(trackOperation, never()).remove(new ArrayList<>());

        // simulate the removal of p2
        when(positionsModel.getPosition(1)).thenReturn(p3);

        trackUpdater.handleRemove(1, 1);

        assertEquals(asList(p1p3, p3p4), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(2, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).remove(asList(p2p3, p1p2));
        verify(trackOperation, times(1)).add(singletonList(p1p3));
    }

    @Test
    public void testRemoveTwoInTheMiddle() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getPosition(3)).thenReturn(p4);
        when(positionsModel.getRowCount()).thenReturn(4);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 3);

        assertEquals(asList(p1p2, p2p3, p3p4), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        assertEquals(2, trackUpdater.getPairWithLayers().get(2).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3, p3p4));
        verify(trackOperation, never()).remove(new ArrayList<>());

        // simulate the removal of p2, p3
        when(positionsModel.getPosition(2)).thenReturn(p4);

        trackUpdater.handleRemove(1, 2);

        assertEquals(singletonList(p1p4), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        verify(trackOperation, times(1)).remove(asList(p3p4, p2p3, p1p2));
        verify(trackOperation, times(1)).add(singletonList(p1p4));
    }

    @Test
    public void testRemoveFromStart() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());

        trackUpdater.handleRemove(0, 0);

        assertEquals(singletonList(p2p3), trackUpdater.getPairWithLayers());
        verify(trackOperation, times(1)).remove(singletonList(p1p2));
        verify(trackOperation, never()).add(new ArrayList<>());
    }

    @Test
    public void testRemoveUntilEnd() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());

        trackUpdater.handleRemove(2, 2);

        assertEquals(singletonList(p1p2), trackUpdater.getPairWithLayers());
        verify(trackOperation, times(1)).remove(singletonList(p2p3));
        verify(trackOperation, never()).add(new ArrayList<>());
    }

    @Test
    public void testRemoveSecondFromPair() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(2);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 1);

        assertEquals(singletonList(p1p2), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        verify(trackOperation, times(1)).add(singletonList(p1p2));
        verify(trackOperation, never()).remove(new ArrayList<>());

        trackUpdater.handleRemove(1, 1);

        assertEquals(new ArrayList<>(), trackUpdater.getPairWithLayers());
        verify(trackOperation, times(1)).remove(singletonList(p1p2));
        verify(trackOperation, never()).add(new ArrayList<>());
    }

    @Test
    public void testRemoveAll() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());

        trackUpdater.handleRemove(0, MAX_VALUE);

        assertEquals(new ArrayList<>(), trackUpdater.getPairWithLayers());
        verify(trackOperation, never()).add(new ArrayList<>());
        verify(trackOperation, times(1)).remove(asList(p2p3, p1p2));
    }

    @Test
    public void testUpdate() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(0)).thenReturn(p1);
        when(positionsModel.getPosition(1)).thenReturn(p2);
        when(positionsModel.getPosition(2)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(0, 2);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());

        trackUpdater.handleUpdate(1, 1);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, times(1)).update(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());
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

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(1).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());

        trackUpdater.handleUpdate(0, MAX_VALUE);

        assertEquals(asList(p1p2, p2p3), trackUpdater.getPairWithLayers());
        assertEquals(0, trackUpdater.getPairWithLayers().get(0).getRow());
        assertEquals(1, trackUpdater.getPairWithLayers().get(01).getRow());
        verify(trackOperation, times(1)).add(asList(p1p2, p2p3));
        verify(trackOperation, times(1)).update(asList(p1p2, p2p3));
        verify(trackOperation, never()).remove(new ArrayList<>());
    }
}
