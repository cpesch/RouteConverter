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
package slash.navigation.mapview.mapsforge;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static slash.navigation.mapview.mapsforge.AwtGraphicMapView.GRAPHIC_FACTORY;

public class BorderPainterTest {
    private MapViewLayerOperations operations;
    private BorderPainter painter;

    @Before
    public void setUp() {
        operations = mock(MapViewLayerOperations.class);
        when(operations.getTileSize()).thenReturn(256);
        when(operations.asLatLong(anyList())).thenReturn(asList(new LatLong(1.0, 2.0), new LatLong(3.0, 4.0)));
        painter = new BorderPainter(operations, GRAPHIC_FACTORY);
    }

    @Test
    public void withoutRouteDrawsOnlyMapBorderAndCentersOnMap() {
        BoundingBox map = box(0.0, 0.0, 10.0, 10.0);
        when(operations.getRouteBoundingBox()).thenReturn(null);

        painter.showMapBorder(map);

        verify(operations, times(1)).addLayer(any(Layer.class));
        verify(operations).centerAndZoom(eq(map), eq(map), eq(true), eq(true));
    }

    @Test
    public void withContainedRouteDrawsBothBordersAndCentersOnMap() {
        BoundingBox map = box(0.0, 0.0, 10.0, 10.0);
        BoundingBox route = box(2.0, 2.0, 5.0, 5.0);
        when(operations.getRouteBoundingBox()).thenReturn(route);

        painter.showMapBorder(map);

        verify(operations, times(2)).addLayer(any(Layer.class));
        verify(operations).centerAndZoom(eq(map), eq(map), eq(true), eq(true));
    }

    @Test
    public void withOutlyingRouteCentersOnRoute() {
        BoundingBox map = box(0.0, 0.0, 3.0, 3.0);
        BoundingBox route = box(2.0, 2.0, 20.0, 20.0);
        when(operations.getRouteBoundingBox()).thenReturn(route);

        painter.showMapBorder(map);

        verify(operations).centerAndZoom(eq(map), eq(route), eq(true), eq(true));
    }

    @Test
    public void redrawRemovesPreviousBorders() {
        BoundingBox map = box(0.0, 0.0, 10.0, 10.0);
        BoundingBox route = box(2.0, 2.0, 5.0, 5.0);
        when(operations.getRouteBoundingBox()).thenReturn(route);

        painter.showMapBorder(map);
        painter.showMapBorder(map);

        // second call removes the two borders drawn by the first
        verify(operations, times(2)).removeLayer(any(Layer.class));
    }

    @Test
    public void nullBoundingBoxRemovesBordersWithoutDrawing() {
        BoundingBox map = box(0.0, 0.0, 10.0, 10.0);
        when(operations.getRouteBoundingBox()).thenReturn(null);
        painter.showMapBorder(map);
        reset(operations);

        painter.showMapBorder(null);

        verify(operations).removeLayer(any(Layer.class));
        verify(operations, never()).addLayer(any(Layer.class));
        verify(operations, never()).centerAndZoom(any(), any(), anyBoolean(), anyBoolean());
    }

    private BoundingBox box(double swLon, double swLat, double neLon, double neLat) {
        NavigationPosition southWest = new SimpleNavigationPosition(swLon, swLat);
        NavigationPosition northEast = new SimpleNavigationPosition(neLon, neLat);
        return new BoundingBox(northEast, southWest);
    }
}
