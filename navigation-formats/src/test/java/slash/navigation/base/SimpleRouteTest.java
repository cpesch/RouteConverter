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

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Tests the shared accessors, equality and the cross-format {@code asXxxFormat} conversion
 * matrix of {@link SimpleRoute} through the concrete {@link Wgs84Route}. A null format is
 * safe - the conversions only use it to construct the (stored) target route.
 *
 * @author Christian Pesch
 */
public class SimpleRouteTest {
    private final Wgs84Position p1 = new Wgs84Position(13.5, 48.5, 100.0, 5.0, fromMillis(1000), "a");
    private final Wgs84Position p2 = new Wgs84Position(14.0, 49.0, 110.0, 6.0, fromMillis(2000), "b");

    private Wgs84Route route(String name) {
        return new Wgs84Route(null, Waypoints, name, new ArrayList<>(asList(p1, p2)));
    }

    @Test
    public void accessorsAndAdd() {
        Wgs84Route route = route("myroute");

        assertEquals("myroute", route.getName());
        assertEquals(2, route.getPositionCount());
        assertSame(p1, route.getPosition(0));
        assertNull(route.getDescription());

        route.add(1, new Wgs84Position(15.0, 50.0, null, null, null, "c"));
        assertEquals(3, route.getPositionCount());
        assertEquals("c", route.getPosition(1).getDescription());
    }

    @Test
    public void nameFallsBackToAGeneratedNameWhenUnset() {
        assertNotNull(route(null).getName());
    }

    @Test
    public void equalsByNameCharacteristicsAndPositions() {
        assertEquals(route("r"), route("r"));
        assertEquals(route("r").hashCode(), route("r").hashCode());
        assertNotEquals(route("r"), route("other"));

        Wgs84Route differentCharacteristics = new Wgs84Route(null, Track, "r", new ArrayList<>(asList(p1, p2)));
        assertNotEquals(route("r"), differentCharacteristics);
    }

    @Test
    public void coordinatePreservingConversionsKeepPositionsAndLongitude() {
        Wgs84Route route = route("r");

        assertConversion(route.asCsvFormat(null));
        assertConversion(route.asGpxFormat(null));
        assertConversion(route.asKmlFormat(null));
        assertConversion(route.asNmeaFormat(null));
        assertConversion(route.asNmnFormat(null));
        assertConversion(route.asPhotoFormat(null));
        assertConversion(route.asSimpleFormat(null));
        assertConversion(route.asTcxFormat(null));
    }

    @Test
    public void formatSpecificConversionsKeepThePositionCount() {
        Wgs84Route route = route("r");

        assertEquals(2, route.asBcrFormat(null).getPositionCount());
        assertEquals(2, route.asGoPalRouteFormat(null).getPositionCount());
        assertEquals(2, route.asTomTomRouteFormat(null).getPositionCount());
    }

    private static void assertConversion(BaseRoute<?, ?> converted) {
        assertEquals(2, converted.getPositionCount());
        assertEquals(13.5, converted.getPosition(0).getLongitude(), 1e-6);
    }
}
