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
package slash.navigation.mapview.mapsforge.renderer;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE;

public class MagnifierPainterTest {
    private MapViewLayerOperations operations;
    private MagnifierPainter painter;

    @Before
    public void setUp() {
        operations = mock(MapViewLayerOperations.class);
        when(operations.asLatLong(any(NavigationPosition.class))).thenReturn(new LatLong(1.0, 2.0));
        painter = new MagnifierPainter(operations, INSTANCE);
    }

    @Test
    public void addsOneMarkerPerPosition() {
        painter.showPositionMagnifier(asList(position(), position()));

        verify(operations).addLayers(argThat((List<Layer> layers) -> layers.size() == 2));
        verify(operations, never()).removeLayers(anyList());
    }

    @Test
    public void removesPreviousMarkersBeforeAddingNewOnes() {
        // the painter passes its markers field to removeLayers and then clears it, so the
        // removed count must be captured at call time rather than asserted at verify time
        List<Integer> removedSizes = new ArrayList<>();
        doAnswer(invocation -> {
            removedSizes.add(((List<?>) invocation.getArgument(0)).size());
            return null;
        }).when(operations).removeLayers(anyList());

        painter.showPositionMagnifier(asList(position(), position()));
        painter.showPositionMagnifier(singletonPositions());

        assertEquals(asList(2), removedSizes);
        verify(operations, times(2)).addLayers(anyList());
    }

    @Test
    public void nullPositionsClearWithoutAdding() {
        painter.showPositionMagnifier(asList(position(), position()));
        reset(operations);

        painter.showPositionMagnifier(null);

        verify(operations).removeLayers(anyList());
        verify(operations, never()).addLayers(anyList());
    }

    @Test
    public void emptyPositionsDoNotAdd() {
        painter.showPositionMagnifier(emptyList());

        verify(operations, never()).addLayers(anyList());
        verify(operations, never()).removeLayers(anyList());
    }

    private List<NavigationPosition> singletonPositions() {
        return asList(position());
    }

    private NavigationPosition position() {
        return new SimpleNavigationPosition(2.0, 1.0);
    }
}
