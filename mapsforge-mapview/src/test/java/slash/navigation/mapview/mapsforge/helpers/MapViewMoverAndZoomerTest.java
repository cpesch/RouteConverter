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
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MapViewMoverAndZoomerTest {

    /**
     * Fake Marker implementation for testing - avoids needing a real Bitmap
     * and MapsforgeMapView which require a live rendering context.
     */
    private static class FakeMarker extends Marker {
        private final boolean tapResult;

        public FakeMarker(LatLong position, boolean tapResult) {
            super(position, null, 0, 0);
            this.tapResult = tapResult;
        }

        @Override
        public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point layerXY,
                            org.mapsforge.core.model.Point tapXY) {
            return tapResult;
        }
    }

    /**
     * Fake DraggableMarker for testing - extends FakeMarker to implement the interface.
     */
    private static class FakeDraggableMarker extends FakeMarker implements DraggableMarker {
        public FakeDraggableMarker(LatLong position, boolean tapResult) {
            super(position, tapResult);
        }

        @Override
        public void onDrop(LatLong latLong) {
            // No-op for test
        }
    }

    private final MapViewProjection projection = mock(MapViewProjection.class);
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
}
