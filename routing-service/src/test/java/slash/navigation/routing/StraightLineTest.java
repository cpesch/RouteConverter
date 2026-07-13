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

package slash.navigation.routing;

import org.junit.Test;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.routing.RoutingResult.Validity.Invalid;
import static slash.navigation.routing.TravelRestrictions.NO_RESTRICTIONS;

public class StraightLineTest {

    private final StraightLine straightLine = new StraightLine();

    @Test
    public void testGetName() {
        assertEquals("StraightLine", straightLine.getName());
    }

    @Test
    public void testIsInitialized() {
        assertTrue(straightLine.isInitialized());
    }

    @Test
    public void testIsDownload() {
        assertFalse(straightLine.isDownload());
    }

    @Test
    public void testGetAvailableTravelModes() {
        List<TravelMode> modes = straightLine.getAvailableTravelModes();
        assertNotNull(modes);
        assertEquals(1, modes.size());
        assertEquals("StraightLine", modes.get(0).name());
    }

    @Test
    public void testGetPreferredTravelMode() {
        assertEquals("StraightLine", straightLine.getPreferredTravelMode().name());
    }

    @Test
    public void testGetAvailableTravelRestrictions() {
        assertEquals(NO_RESTRICTIONS, straightLine.getAvailableTravelRestrictions());
    }

    @Test
    public void testGetRouteBetweenReturnsInvalidWithTwoPositions() {
        SimpleNavigationPosition from = new SimpleNavigationPosition(0.0, 0.0);
        SimpleNavigationPosition to = new SimpleNavigationPosition(1.0, 1.0);
        RoutingResult result = StraightLine.getRouteBetween(from, to);
        assertNotNull(result);
        assertEquals(Invalid, result.validity());
        assertEquals(2, result.positions().size());
    }

    @Test
    public void testGetRouteBetweenDistanceIsPositive() {
        SimpleNavigationPosition from = new SimpleNavigationPosition(8.0, 47.0);
        SimpleNavigationPosition to = new SimpleNavigationPosition(9.0, 48.0);
        RoutingResult result = StraightLine.getRouteBetween(from, to);
        assertNotNull(result.distanceAndTime());
        assertTrue("Distance should be positive", result.distanceAndTime().distance() > 0);
    }

    @Test
    public void testGetRouteBetweenSamePositionZeroDistance() {
        SimpleNavigationPosition pos = new SimpleNavigationPosition(10.0, 50.0);
        RoutingResult result = StraightLine.getRouteBetween(pos, pos);
        assertEquals(0.0, result.distanceAndTime().distance(), 0.001);
    }

    @Test
    public void testGetSnapToRoadPositionReturnsNull() {
        SimpleNavigationPosition pos = new SimpleNavigationPosition(10.0, 50.0);
        assertNull(straightLine.getSnapToRoadPosition(pos));
    }

    @Test
    public void testInstanceMethodDelegatesToStatic() {
        SimpleNavigationPosition from = new SimpleNavigationPosition(8.0, 47.0);
        SimpleNavigationPosition to = new SimpleNavigationPosition(9.0, 48.0);
        RoutingResult via_static = StraightLine.getRouteBetween(from, to);
        RoutingResult via_instance = straightLine.getRouteBetween(from, to, straightLine.getPreferredTravelMode(), NO_RESTRICTIONS);
        assertEquals(via_static.validity(), via_instance.validity());
        assertEquals(via_static.distanceAndTime().distance(), via_instance.distanceAndTime().distance(), 0.001);
    }
}

