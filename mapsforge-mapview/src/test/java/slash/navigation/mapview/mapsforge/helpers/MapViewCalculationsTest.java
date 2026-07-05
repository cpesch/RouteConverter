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
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static slash.navigation.mapview.mapsforge.helpers.MapViewCalculations.*;

public class MapViewCalculationsTest {

    @Test
    public void thresholdScalesWithPixelsAndShrinksWithZoom() {
        double onePixel = thresholdForPixel(0.0, (byte) 10, 256, 1);
        double tenPixels = thresholdForPixel(0.0, (byte) 10, 256, 10);
        assertTrue(onePixel > 0);
        assertEquals(10 * onePixel, tenPixels, 1e-9);

        double zoomedIn = thresholdForPixel(0.0, (byte) 15, 256, 10);
        assertTrue("higher zoom means fewer meters per pixel", zoomedIn < tenPixels);
    }

    @Test
    public void collectRouteOnly() {
        assertEquals(2, collectBoundingPositions(null, box(0, 0, 10, 10)).size());
    }

    @Test
    public void collectMapOnly() {
        assertEquals(2, collectBoundingPositions(box(0, 0, 10, 10), null).size());
    }

    @Test
    public void collectMapContainingRoute() {
        assertEquals(4, collectBoundingPositions(box(0, 0, 10, 10), box(2, 2, 5, 5)).size());
    }

    @Test
    public void collectMapNotContainingRoute() {
        assertEquals(6, collectBoundingPositions(box(0, 0, 3, 3), box(2, 2, 20, 20)).size());
    }

    @Test
    public void collectNeitherIsEmpty() {
        assertTrue(collectBoundingPositions(null, null).isEmpty());
    }

    @Test
    public void addRowAfterSelectedPosition() {
        PositionsModel model = mock(PositionsModel.class);
        NavigationPosition selected = new SimpleNavigationPosition(1.0, 2.0);
        when(model.getIndex(selected)).thenReturn(3);

        assertEquals(4, computeAddRow(selected, model));
    }

    @Test
    public void addRowAfterLastPositionWhenNothingSelected() {
        PositionsModel model = mock(PositionsModel.class);
        NavigationPosition last = new SimpleNavigationPosition(1.0, 2.0);
        when(model.getRowCount()).thenReturn(5);
        when(model.getPosition(4)).thenReturn(last);
        when(model.getIndex(last)).thenReturn(4);

        assertEquals(5, computeAddRow(null, model));
    }

    @Test
    public void addRowAtStartWhenEmpty() {
        PositionsModel model = mock(PositionsModel.class);
        when(model.getRowCount()).thenReturn(0);

        assertEquals(0, computeAddRow(null, model));
    }

    private BoundingBox box(double swLon, double swLat, double neLon, double neLat) {
        NavigationPosition southWest = new SimpleNavigationPosition(swLon, swLat);
        NavigationPosition northEast = new SimpleNavigationPosition(neLon, neLat);
        return new BoundingBox(northEast, southWest);
    }
}
