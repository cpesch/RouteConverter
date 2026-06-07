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

package slash.navigation.base;

import org.junit.Test;

import static org.junit.Assert.*;

public class WaypointTypeTest {

    @Test
    public void testAirportValue() {
        assertEquals("AIRPORT", WaypointType.Airport.value());
    }

    @Test
    public void testUserWaypointValue() {
        assertEquals("USER WAYPOINT", WaypointType.UserWaypoint.value());
    }

    @Test
    public void testIntersectionValue() {
        assertEquals("INT", WaypointType.Intersection.value());
    }

    @Test
    public void testHasMultipleValues() {
        assertTrue("Should have more than 3 waypoint types", WaypointType.values().length > 3);
    }

    @Test
    public void testValueOfRoundTrip() {
        WaypointType original = WaypointType.Airport;
        assertEquals(original, WaypointType.valueOf(original.name()));
    }

    @Test
    public void testFromValueAirport() {
        WaypointType result = WaypointType.fromValue("AIRPORT");
        assertEquals(WaypointType.Airport, result);
    }

    @Test
    public void testFromValueUserWaypoint() {
        WaypointType result = WaypointType.fromValue("USER WAYPOINT");
        assertEquals(WaypointType.UserWaypoint, result);
    }

    @Test
    public void testFromValueUnknownReturnsNull() {
        assertNull(WaypointType.fromValue("UNKNOWN_TYPE_XYZ"));
    }
}

