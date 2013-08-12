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
    private PositionPair a = new PositionPair(p1, p2);
    private PositionPair b = new PositionPair(p2, p3);

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
    public void testAdded() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(1, 2);

        assertEquals(asList(a), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(a));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());
    }

    @Test
    public void testRemoved() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getPosition(2)).thenReturn(p2);
        when(positionsModel.getPosition(3)).thenReturn(p3);
        TrackOperation trackOperation = mock(TrackOperation.class);

        TrackUpdater trackUpdater = new TrackUpdater(positionsModel, trackOperation);
        trackUpdater.handleAdd(1, 3);

        assertEquals(asList(a), trackUpdater.getCurrentTrack());
        verify(trackOperation, times(1)).add(asList(a));
        verify(trackOperation, never()).remove(new ArrayList<PositionPair>());

        trackUpdater.handleRemove(2, 2);

        assertEquals(asList(a), trackUpdater.getCurrentTrack());
        verify(trackOperation, never()).add(new ArrayList<PositionPair>());
        verify(trackOperation, times(1)).remove(asList(b));
    }
}
