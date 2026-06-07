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

public class RouteCharacteristicsTest {

    @Test
    public void testExactlyThreeValues() {
        assertEquals(3, RouteCharacteristics.values().length);
    }

    @Test
    public void testOrdinalRoute() {
        assertEquals(0, RouteCharacteristics.Route.ordinal());
    }

    @Test
    public void testOrdinalTrack() {
        assertEquals(1, RouteCharacteristics.Track.ordinal());
    }

    @Test
    public void testOrdinalWaypoints() {
        assertEquals(2, RouteCharacteristics.Waypoints.ordinal());
    }

    @Test
    public void testValueOfRoundTrip() {
        for (RouteCharacteristics c : RouteCharacteristics.values()) {
            assertEquals(c, RouteCharacteristics.valueOf(c.name()));
        }
    }

    @Test
    public void testNamesMatchExpected() {
        assertEquals("Route", RouteCharacteristics.Route.name());
        assertEquals("Track", RouteCharacteristics.Track.name());
        assertEquals("Waypoints", RouteCharacteristics.Waypoints.name());
    }
}

