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

import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.mapview.PoiMarker;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;

import java.awt.*;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PoiOverlayRendererTest {
    // one pixel per thousandth of a degree, so positions read as pixel coordinates
    private static final Function<NavigationPosition, Point> TO_PIXELS =
            position -> new Point(position.getLongitude() * 1000, position.getLatitude() * 1000);

    private PoiMarker marker(double longitude, double latitude, String label, Color color) {
        return new PoiMarker(new SimpleNavigationPosition(longitude, latitude, null, label), label, color);
    }

    @Test
    public void findsNearestMarkerWithinRadius() {
        PoiMarker near = marker(0.103, 0.100, "near", Color.RED);
        PoiMarker nearer = marker(0.101, 0.100, "nearer", Color.RED);
        PoiMarker far = marker(0.200, 0.100, "far", Color.RED);

        assertSame(nearer, PoiOverlayRenderer.findNearest(List.of(near, nearer, far), TO_PIXELS, new Point(100, 100), 10));
        assertSame(near, PoiOverlayRenderer.findNearest(List.of(near, far), TO_PIXELS, new Point(100, 100), 10));
    }

    @Test
    public void findsNoMarkerOutsideRadiusOrWithoutPoint() {
        PoiMarker far = marker(0.200, 0.100, "far", Color.RED);

        assertNull(PoiOverlayRenderer.findNearest(List.of(far), TO_PIXELS, new Point(100, 100), 10));
        assertNull(PoiOverlayRenderer.findNearest(List.of(far), TO_PIXELS, null, 10));
        assertNull(PoiOverlayRenderer.findNearest(List.of(), TO_PIXELS, new Point(100, 100), 10));
    }

    @Test
    public void showsOneMarkerPerPoiAndCreatesOneIconPerColor() {
        MapsforgeMapView mapView = mock(MapsforgeMapView.class);
        when(mapView.getDisplayModel()).thenReturn(new DisplayModel());
        when(mapView.asLatLong(any(NavigationPosition.class))).thenAnswer(invocation -> {
            NavigationPosition position = invocation.getArgument(0);
            return new LatLong(position.getLatitude(), position.getLongitude());
        });
        when(mapView.createWaypointIcon(anyString(), anyString())).thenReturn(mock(Bitmap.class));
        PoiOverlayRenderer renderer = new PoiOverlayRenderer(mapView);

        renderer.show(List.of(marker(13.0, 52.0, "a", Color.RED), marker(13.1, 52.1, "b", Color.RED),
                marker(13.2, 52.2, "c", Color.BLUE), new PoiMarker(new SimpleNavigationPosition(null, null), "no coordinates", Color.RED)));

        assertEquals(3, renderer.getLayer().layers.size());
        verify(mapView).createWaypointIcon("ff0000", "0.9");
        verify(mapView).createWaypointIcon("0000ff", "0.9");
        verifyNoMoreInteractions(ignoreStubs(mapView));

        renderer.show(null);
        assertTrue(renderer.getLayer().layers.isEmpty());
        assertNull(renderer.getMarkerAt(TO_PIXELS, new Point(13000, 52000), 10));
    }
}
