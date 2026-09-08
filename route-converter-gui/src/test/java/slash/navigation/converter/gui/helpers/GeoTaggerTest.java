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
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.navigation.converter.gui.helpers.GeoTagger.notifyPositionUpdatedOnEventDispatchThread;

public class GeoTaggerTest {
    @Test
    public void notifiesOnEventDispatchThreadForKnownPosition() throws Exception {
        NavigationPosition position = mock(NavigationPosition.class);
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getIndex(position)).thenReturn(3);

        AtomicBoolean firedOnEventDispatchThread = new AtomicBoolean(false);
        AtomicInteger firedFirstIndex = new AtomicInteger(-1);
        AtomicInteger firedLastIndex = new AtomicInteger(-1);
        CountDownLatch fired = new CountDownLatch(1);

        org.mockito.Mockito.doAnswer(invocation -> {
            firedOnEventDispatchThread.set(SwingUtilities.isEventDispatchThread());
            firedFirstIndex.set(invocation.getArgument(0));
            firedLastIndex.set(invocation.getArgument(1));
            fired.countDown();
            return null;
        }).when(positionsModel).fireTableRowsUpdated(anyInt(), anyInt(), org.mockito.ArgumentMatchers.eq(ALL_COLUMNS));

        // called off the EDT, like from the GeoTagger background executor
        notifyPositionUpdatedOnEventDispatchThread(positionsModel, position);

        assertTrue("fireTableRowsUpdated was not invoked", fired.await(5, TimeUnit.SECONDS));
        assertTrue("update was not fired on the EDT", firedOnEventDispatchThread.get());
        assertEquals(3, firedFirstIndex.get());
        assertEquals(3, firedLastIndex.get());
    }

    @Test
    public void doesNotNotifyForUnknownPosition() {
        NavigationPosition position = mock(NavigationPosition.class);
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getIndex(position)).thenReturn(-1);

        notifyPositionUpdatedOnEventDispatchThread(positionsModel, position);

        org.mockito.Mockito.verify(positionsModel, org.mockito.Mockito.never())
                .fireTableRowsUpdated(anyInt(), anyInt(), anyInt());
    }
}
