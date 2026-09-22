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

package slash.navigation.rtz;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.ParserResult;
import slash.navigation.base.Wgs84Position;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

/**
 * Tests for {@link RtzFormat}: reading the 1.0 and 1.1 fixtures, the eta to
 * position time mapping and the write to re-read round-trip.
 */
public class RtzFormatTest extends NavigationTestCase {
    private final NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    private BaseRoute<?, ?> readFile(String fileName) throws IOException {
        ParserResult result = parser.read(new File(TEST_PATH + fileName),
                parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(".rtz"));
        assertNotNull("Cannot read route from " + fileName, result);
        assertTrue(result.isSuccessful());
        assertEquals(RtzFormat.class, result.getFormat().getClass());
        return result.getTheRoute();
    }

    private BaseRoute<?, ?> readRtz(String rtz) throws IOException {
        ParserResult result = parser.read(new ByteArrayInputStream(rtz.getBytes(StandardCharsets.UTF_8)),
                parser.getNavigationFormatRegistry().getReadFormatsPreferredByExtension(".rtz"));
        assertNotNull("Cannot read route", result);
        assertTrue(result.isSuccessful());
        assertEquals(RtzFormat.class, result.getFormat().getClass());
        return result.getTheRoute();
    }

    private static long utcMillis(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Test
    public void testReadFixtureWithNamesAndOneEta() throws IOException {
        BaseRoute<?, ?> route = readFile("from.rtz");
        assertEquals(Route, route.getCharacteristics());
        assertEquals("Elbe estuary", route.getName());
        assertEquals(3, route.getPositionCount());

        assertEquals("Elbe 1", route.getPosition(0).getDescription());
        assertEquals(53.995, route.getPosition(0).getLatitude(), 0.000001);
        assertEquals(8.7133, route.getPosition(0).getLongitude(), 0.000001);
        assertNull(route.getPosition(0).getTime());

        assertEquals("Elbe 4", route.getPosition(1).getDescription());
        assertNotNull(route.getPosition(1).getTime());
        assertEquals(utcMillis(2026, Calendar.SEPTEMBER, 10, 9, 0, 0),
                route.getPosition(1).getTime().getCalendar().getTimeInMillis());

        assertEquals("Cuxhaven", route.getPosition(2).getDescription());
        assertNull(route.getPosition(2).getTime());
    }

    @Test
    public void testReadRtz11Fixture() throws IOException {
        BaseRoute<?, ?> route = readFile("from-rtz11.rtz");
        assertEquals("Elbe estuary", route.getName());
        assertEquals(3, route.getPositionCount());
        assertEquals("Elbe 1", route.getPosition(0).getDescription());
        assertNotNull(route.getPosition(1).getTime());
    }

    @Test
    public void testReadWithoutSchedulesAndWithoutNames() throws IOException {
        BaseRoute<?, ?> route = readRtz("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<route xmlns=\"http://www.cirm.org/RTZ/1/0\" version=\"1.0\">" +
                "<waypoints>" +
                "<waypoint id=\"1\"><position lat=\"53.9950\" lon=\"8.7133\"/></waypoint>" +
                "<waypoint id=\"2\"><position lat=\"53.8700\" lon=\"9.0500\"/></waypoint>" +
                "</waypoints>" +
                "</route>");
        // without a routeInfo route name the route gets a generated name
        assertEquals("Position 1 to Position 2", route.getName());
        assertEquals(2, route.getPositionCount());
        // waypoints without a name are commented when reading
        assertEquals("Position 1", route.getPosition(0).getDescription());
        assertNull(route.getPosition(0).getTime());
        assertNull(route.getPosition(1).getTime());
    }

    @Test
    public void testWriteThenRereadEquals() throws IOException {
        RtzFormat format = new RtzFormat();
        CompactCalendar eta = CompactCalendar.fromMillis(utcMillis(2026, Calendar.SEPTEMBER, 10, 9, 0, 0));
        List<Wgs84Position> positions = asList(
                new Wgs84Position(8.7133512345, 53.995, null, null, eta, "Elbe 1"),
                new Wgs84Position(9.05, 53.87, null, null, null, "Elbe 4"),
                new Wgs84Position(8.7, 53.86, null, null, null, null));
        RtzRoute route = format.createRoute(Route, "Test route", positions);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        format.write(route, outputStream, 0, positions.size());

        BaseRoute<?, ?> reread = readRtz(outputStream.toString(StandardCharsets.UTF_8));
        assertEquals("Test route", reread.getName());
        assertEquals(3, reread.getPositionCount());
        assertEquals("Elbe 1", reread.getPosition(0).getDescription());
        assertEquals(8.713351, reread.getPosition(0).getLongitude(), 0.0000001);
        assertEquals(utcMillis(2026, Calendar.SEPTEMBER, 10, 9, 0, 0),
                reread.getPosition(0).getTime().getCalendar().getTimeInMillis());
        assertEquals("Elbe 4", reread.getPosition(1).getDescription());
        assertNull(reread.getPosition(1).getTime());
        // a position without description is commented when writing
        assertEquals("Position 3", reread.getPosition(2).getDescription());
        assertNull(reread.getPosition(2).getTime());
    }
}
