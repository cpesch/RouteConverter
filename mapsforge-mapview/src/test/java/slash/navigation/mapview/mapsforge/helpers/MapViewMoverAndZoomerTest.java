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
package slash.navigation.mapview.mapsforge.helpers;

import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.mapview.mapsforge.AwtGraphicMapView;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;

import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static java.awt.event.MouseEvent.MOUSE_DRAGGED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MapViewMoverAndZoomerTest {

    /**
     * Fake DraggableMarker for testing - extends DraggableMarker directly.
     * The real DraggableMarker requires MapsforgeMapView and PositionWithLayer which
     * we don't have in a unit test, so we subclass to override onTap for hit testing.
     */
    private static class FakeDraggableMarker extends DraggableMarker {
        private final boolean tapResult;

        public FakeDraggableMarker(LatLong position, boolean tapResult) {
            super(null, null, position, null, 0, 0);
            this.tapResult = tapResult;
        }

        @Override
        public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY,
                            org.mapsforge.core.model.Point tapXY) {
            return tapResult;
        }

        @Override
        public void onDrop(LatLong latLong) {
            // No-op for test
        }
    }

    private final MapViewProjection projection = mock(MapViewProjection.class);
    private final AwtGraphicMapView mapView = mock(AwtGraphicMapView.class);
    private final LayerManager layerManager = mock(LayerManager.class);
    private final LatLong draggedTo = new LatLong(11.0, 21.0);
    private final Container eventSource = new Container();
    private final LatLong tapLatLong = new LatLong(10.0, 20.0);
    private final org.mapsforge.core.model.Point tapXY = new org.mapsforge.core.model.Point(100, 200);

    @Test
    public void testMarkerAtTapPointFound() {
        LatLong markerPos = new LatLong(10.0, 20.0);
        FakeDraggableMarker marker = spy(new FakeDraggableMarker(markerPos, true));

        when(projection.toPixels(markerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(marker);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        assertSame(marker, result);
    }

    @Test
    public void testMarkerInGroupLayerAtTapPointFound() {
        LatLong markerPos = new LatLong(10.0, 20.0);
        FakeDraggableMarker marker = spy(new FakeDraggableMarker(markerPos, true));
        GroupLayer groupLayer = new GroupLayer();
        groupLayer.layers.add(marker);

        when(projection.toPixels(markerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(groupLayer);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        assertSame(marker, result);
    }

    @Test
    public void testNoMarkerAtTapPointReturnsNull() {
        LatLong markerPos = new LatLong(10.0, 20.0);
        FakeDraggableMarker marker = spy(new FakeDraggableMarker(markerPos, false));

        when(projection.toPixels(markerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(marker);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        assertNull(result);
    }

    @Test
    public void testEmptyListReturnsNull() {
        ArrayList<Layer> layers = new ArrayList<>();

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        assertNull(result);
    }

    @Test
    public void testNonMarkerLayerIsSkipped() {
        Layer nonMarkerLayer = mock(Layer.class);

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(nonMarkerLayer);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        assertNull(result);
    }

    @Test
    public void testTopmostMarkerWinsWhenOverlapping() {
        LatLong markerPos1 = new LatLong(10.0, 20.0);
        LatLong markerPos2 = new LatLong(10.0, 20.0);
        FakeDraggableMarker marker1 = spy(new FakeDraggableMarker(markerPos1, true));
        FakeDraggableMarker marker2 = spy(new FakeDraggableMarker(markerPos2, true));

        when(projection.toPixels(markerPos1)).thenReturn(new org.mapsforge.core.model.Point(100, 200));
        when(projection.toPixels(markerPos2)).thenReturn(new org.mapsforge.core.model.Point(100, 200));

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(marker1);
        layers.add(marker2);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        // Last marker in list should win (topmost in z-order)
        assertSame(marker2, result);
    }

    @Test
    public void testNestedGroupLayerMarkerWinsOverFlatMarkerWhenLaterInList() {
        LatLong flatMarkerPos = new LatLong(10.0, 20.0);
        LatLong nestedMarkerPos = new LatLong(10.0, 20.0);
        FakeDraggableMarker flatMarker = spy(new FakeDraggableMarker(flatMarkerPos, true));
        FakeDraggableMarker nestedMarker = spy(new FakeDraggableMarker(nestedMarkerPos, true));

        GroupLayer groupLayer = new GroupLayer();
        groupLayer.layers.add(nestedMarker);

        when(projection.toPixels(flatMarkerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));
        when(projection.toPixels(nestedMarkerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(flatMarker);
        layers.add(groupLayer);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        // Nested marker should win because its GroupLayer comes later in the list
        assertSame(nestedMarker, result);
    }

    @Test
    public void testFlatMarkerWinsOverNestedGroupLayerMarkerWhenLaterInList() {
        LatLong flatMarkerPos = new LatLong(10.0, 20.0);
        LatLong nestedMarkerPos = new LatLong(10.0, 20.0);
        FakeDraggableMarker flatMarker = spy(new FakeDraggableMarker(flatMarkerPos, true));
        FakeDraggableMarker nestedMarker = spy(new FakeDraggableMarker(nestedMarkerPos, true));

        GroupLayer groupLayer = new GroupLayer();
        groupLayer.layers.add(nestedMarker);

        when(projection.toPixels(flatMarkerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));
        when(projection.toPixels(nestedMarkerPos)).thenReturn(new org.mapsforge.core.model.Point(100, 200));

        ArrayList<Layer> layers = new ArrayList<>();
        layers.add(groupLayer);
        layers.add(flatMarker);

        DraggableMarker result = MapViewMoverAndZoomer.findDraggableMarkerAt(layers, projection, tapLatLong, tapXY);

        // Flat marker should win because it comes later in the list
        assertSame(flatMarker, result);
    }

    /**
     * Dragging a selection marker must repaint the map. The marker's own requestRedraw() cannot:
     * only Layers#add assigns a Redrawer, and selection markers are children of a GroupLayer
     * (#357), so the dragged pin stayed frozen at its old position until the drop while only the
     * drag cursor moved -- reported 2026-09-16.
     */
    @Test
    public void testDraggingAMarkerRedrawsTheMap() {
        FakeDraggableMarker marker = draggableMarkerUnderTheCursor();
        MapViewMoverAndZoomer moverAndZoomer = new MapViewMoverAndZoomer(mapView, layerManager, projection);

        moverAndZoomer.mousePressed(mouseEvent(MOUSE_PRESSED, 100, 200));
        assertTrue(moverAndZoomer.isMousePressedOnMarker());

        moverAndZoomer.mouseDragged(mouseEvent(MOUSE_DRAGGED, 140, 260));

        verify(layerManager).redrawLayers();
        assertEquals(draggedTo, marker.getLatLong());
    }

    /**
     * The pin keeps the offset it was grabbed with, so its tip decides where the position lands --
     * as brouter-web, graphhopper and kurviger do, and as RouteConverter did up to 3.6. Grabbing
     * the pin by its head must not make it jump so that its tip sits under the pointer.
     */
    @Test
    public void testDraggingAMarkerKeepsTheGrabOffset() {
        FakeDraggableMarker marker = draggableMarkerUnderTheCursor();
        MapViewMoverAndZoomer moverAndZoomer = new MapViewMoverAndZoomer(mapView, layerManager, projection);

        // grab the marker 25px above its anchor, as one does when grabbing a pin by its head
        moverAndZoomer.mousePressed(mouseEvent(MOUSE_PRESSED, 100, 175));
        moverAndZoomer.mouseDragged(mouseEvent(MOUSE_DRAGGED, 140, 260));

        // the anchor stays 25px below the pointer: 260 + (200 - 175)
        verify(projection).fromPixels(140.0, 285.0);
        assertEquals(draggedTo, marker.getLatLong());
    }

    private FakeDraggableMarker draggableMarkerUnderTheCursor() {
        LatLong markerPosition = new LatLong(10.0, 20.0);
        FakeDraggableMarker marker = new FakeDraggableMarker(markerPosition, true);

        Layers layers = mock(Layers.class);
        when(layers.getLayers()).thenReturn(List.of(marker));
        when(layerManager.getLayers()).thenReturn(layers);
        when(projection.toPixels(markerPosition)).thenReturn(new org.mapsforge.core.model.Point(100, 200));
        when(projection.fromPixels(anyDouble(), anyDouble())).thenReturn(tapLatLong);
        when(projection.fromPixels(140.0, 260.0)).thenReturn(draggedTo);
        when(projection.fromPixels(140.0, 285.0)).thenReturn(draggedTo);
        return marker;
    }

    private MouseEvent mouseEvent(int id, int x, int y) {
        // a mocked Component cannot source a MouseEvent: the constructor reads its screen location
        return new MouseEvent(eventSource, id, 0L, BUTTON1_DOWN_MASK, x, y, 1, false);
    }
}
