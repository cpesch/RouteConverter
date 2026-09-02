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
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SelectionUpdaterTest {
    private final NavigationPosition p1 = new SimpleNavigationPosition(1.0, 0.0);
    private final NavigationPosition p2 = new SimpleNavigationPosition(2.0, 0.0);
    private final NavigationPosition p3 = new SimpleNavigationPosition(3.0, 0.0);

    private final PositionWithLayer w1 = new PositionWithLayer(p1);
    private final PositionWithLayer w2 = new PositionWithLayer(p2);
    private final PositionWithLayer w3 = new PositionWithLayer(p3);

    @Test
    public void testNavigationPositionEqualsAndHashCode() {
        assertEquals(p1.hashCode(), p1.hashCode());
        assertNotEquals(p1.hashCode(), p2.hashCode());
        assertEquals(p1, p1);
        assertNotEquals(p1, p2);
        Set<NavigationPosition> set = new HashSet<>();
        set.add(p1);
        assertTrue(set.contains(p1));
        assertFalse(set.contains(p2));
    }

    @Test
    public void testInitiallyEmpty() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        SelectionOperation selectionOperation = mock(SelectionOperation.class);

        SelectionUpdater selectionUpdater = new SelectionUpdater(positionsModel, selectionOperation);

        assertTrue(selectionUpdater.getPositionWithLayers().isEmpty());
        verify(selectionOperation, never()).add(new ArrayList<PositionWithLayer>());
        verify(selectionOperation, never()).remove(new ArrayList<PositionWithLayer>());
    }

    @Test
    public void testAdded() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getRowCount()).thenReturn(2);
        SelectionOperation selectionOperation = mock(SelectionOperation.class);

        SelectionUpdater selectionUpdater = new SelectionUpdater(positionsModel, selectionOperation);
        selectionUpdater.setSelectedPositions(new int[]{1}, false);

        assertEquals(singletonList(w1), selectionUpdater.getPositionWithLayers());
        verify(selectionOperation, times(1)).add(singletonList(w1));
        verify(selectionOperation, never()).remove(new ArrayList<PositionWithLayer>());
    }

    @Test
    public void testRemoved() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getPosition(2)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(3);
        SelectionOperation selectionOperation = mock(SelectionOperation.class);

        SelectionUpdater selectionUpdater = new SelectionUpdater(positionsModel, selectionOperation);
        selectionUpdater.setSelectedPositions(new int[]{1, 2}, false);

        assertEquals(asList(w1, w2), selectionUpdater.getPositionWithLayers());
        verify(selectionOperation, times(1)).add(asList(w1, w2));
        verify(selectionOperation, never()).remove(new ArrayList<PositionWithLayer>());

        selectionUpdater.setSelectedPositions(new int[]{1}, false);

        assertEquals(singletonList(w1), selectionUpdater.getPositionWithLayers());
        verify(selectionOperation, never()).add(new ArrayList<PositionWithLayer>());
        verify(selectionOperation, times(1)).remove(singletonList(w2));
    }

    /**
     * A {@link SelectionOperation} that behaves like the real one in MapsforgeMapView: it attaches
     * a {@link Layer} to every position it renders. A mock never does, which is why the additive
     * path used to pass with a broken PositionWithLayer#equals - see issue #358.
     */
    private static class RenderingSelectionOperation implements SelectionOperation {
        private final List<List<PositionWithLayer>> added = new ArrayList<>();
        private final List<List<PositionWithLayer>> removed = new ArrayList<>();

        public void add(List<PositionWithLayer> positionWithLayers) {
            added.add(new ArrayList<>(positionWithLayers));
            for (PositionWithLayer positionWithLayer : positionWithLayers)
                positionWithLayer.setLayer(mock(Layer.class));
        }

        public void remove(List<PositionWithLayer> positionWithLayers) {
            removed.add(new ArrayList<>(positionWithLayers));
        }
    }

    @Test
    public void testAddedToAnAlreadyRenderedSelection() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getPosition(2)).thenReturn(p2);
        when(positionsModel.getPosition(3)).thenReturn(p3);
        when(positionsModel.getRowCount()).thenReturn(4);
        RenderingSelectionOperation selectionOperation = new RenderingSelectionOperation();

        SelectionUpdater selectionUpdater = new SelectionUpdater(positionsModel, selectionOperation);
        selectionUpdater.setSelectedPositions(new int[]{1, 2}, false);

        assertEquals(asList(w1, w2), selectionUpdater.getPositionWithLayers());
        assertEquals(singletonList(asList(w1, w2)), selectionOperation.added);
        assertTrue(selectionOperation.removed.isEmpty());

        // the stored entries now carry a Layer, the candidates built from the model do not
        selectionUpdater.setSelectedPositions(new int[]{1, 2, 3}, false);

        assertEquals(asList(w1, w2, w3), selectionUpdater.getPositionWithLayers());
        assertEquals("only the new position may be rendered again",
                asList(asList(w1, w2), singletonList(w3)), selectionOperation.added);
        assertTrue("nothing was deselected, so nothing may be removed", selectionOperation.removed.isEmpty());
    }

    @Test
    public void testRemovedFromAnAlreadyRenderedSelection() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getPosition(2)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(3);
        RenderingSelectionOperation selectionOperation = new RenderingSelectionOperation();

        SelectionUpdater selectionUpdater = new SelectionUpdater(positionsModel, selectionOperation);
        selectionUpdater.setSelectedPositions(new int[]{1, 2}, false);
        selectionUpdater.setSelectedPositions(new int[]{1}, false);

        assertEquals(singletonList(w1), selectionUpdater.getPositionWithLayers());
        assertEquals("the position that stayed selected must not be rendered twice",
                singletonList(asList(w1, w2)), selectionOperation.added);
        assertEquals(singletonList(singletonList(w2)), selectionOperation.removed);
    }

    @Test
    public void testRemovedReplaceSelection() {
        PositionsModel positionsModel = mock(PositionsModel.class);
        when(positionsModel.getPosition(1)).thenReturn(p1);
        when(positionsModel.getPosition(2)).thenReturn(p2);
        when(positionsModel.getRowCount()).thenReturn(3);
        SelectionOperation selectionOperation = mock(SelectionOperation.class);

        SelectionUpdater selectionUpdater = new SelectionUpdater(positionsModel, selectionOperation);
        selectionUpdater.setSelectedPositions(new int[]{1, 2}, false);

        assertEquals(asList(w1, w2), selectionUpdater.getPositionWithLayers());
        verify(selectionOperation, times(1)).add(asList(w1, w2));
        verify(selectionOperation, never()).remove(new ArrayList<PositionWithLayer>());

        selectionUpdater.setSelectedPositions(new int[]{1, 2}, true);

        assertEquals(asList(w1, w2), selectionUpdater.getPositionWithLayers());
        verify(selectionOperation, times(2)).add(asList(w1, w2));
        verify(selectionOperation, times(1)).remove(asList(w1, w2));
    }
}
