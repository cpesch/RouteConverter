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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.geonames;

import junit.framework.TestCase;

import java.io.IOException;

public class GeoNamesServiceTest extends TestCase {
    private final GeoNamesService service = new GeoNamesService();

    public void testSrtm3ElevationLookup() throws IOException {
        assertEquals(209, (int) service.getSrtm3ElevationFor(10.2, 50.001));
        assertEquals(null, service.getSrtm3ElevationFor(11.06561, 47.42428));
        assertEquals(null, service.getSrtm3ElevationFor(0.0, 0.0));

        assertEquals(40, (int) service.getSrtm3ElevationFor(11.2, 59.0));
        assertEquals(190, (int) service.getSrtm3ElevationFor(11.2, 60.0));
        assertEquals(null, service.getSrtm3ElevationFor(11.2, 61.0));

        assertEquals(77, (int) service.getSrtm3ElevationFor(-68.0, -54.0));
        assertEquals(455, (int) service.getSrtm3ElevationFor(-68.0, -55.0));
        assertEquals(0, (int)service.getSrtm3ElevationFor(-68.0, -56.0));
        assertEquals(null, service.getSrtm3ElevationFor(-68.0, -56.1));
        assertEquals(null, service.getSrtm3ElevationFor(-68.0, -57.0));
    }

    public void testGtopo30ElevationLookup() throws IOException {
        assertEquals(205, (int) service.getGtopo30ElevationFor(10.2, 50.001));
        assertEquals(1789, (int) service.getGtopo30ElevationFor(11.06561, 47.42428));
        assertEquals(null, service.getGtopo30ElevationFor(0.0, 0.0));

        assertEquals(17, (int) service.getGtopo30ElevationFor(11.2, 59.0));
        assertEquals(120, (int) service.getGtopo30ElevationFor(11.2, 60.0));
        assertEquals(648, (int) service.getGtopo30ElevationFor(11.2, 61.0));

        assertEquals(119, (int) service.getGtopo30ElevationFor(-68.0, -54.0));
        assertEquals(184, (int) service.getGtopo30ElevationFor(-68.0, -55.0));
        assertEquals(null, service.getGtopo30ElevationFor(-68.0, -56.0));
        assertEquals(null, service.getGtopo30ElevationFor(-68.0, -56.1));
        assertEquals(null, service.getGtopo30ElevationFor(-68.0, -57.0));
    }

    public void testElevationLookup() throws IOException {
        assertEquals(40, (int) service.getElevationFor(11.2, 59.0));
        assertEquals(120, (int) service.getElevationFor(11.2, 60.0));
        assertEquals(648, (int) service.getElevationFor(11.2, 61.0));

        assertEquals(77, (int) service.getElevationFor(-68.0, -54.0));
        assertEquals(455, (int) service.getElevationFor(-68.0, -55.0));
        assertEquals(null, service.getElevationFor(-68.0, -56.0));
        assertEquals(null, service.getElevationFor(-68.0, -56.1));
        assertEquals(null, service.getElevationFor(-68.0, -57.0));
    }

    public void testNearByLookup() throws IOException {
        // was: assertEquals("Kreuzegg", service.getNearByFor(9.0, 47.3));
        assertEquals("Atzmännig", service.getNearByFor(9.0, 47.3));
        assertEquals("Grafenrheinfeld", service.getNearByFor(10.2, 50.001));
        // was: assertEquals("Hoher Gaif", service.getNearByFor(11.06561, 47.42428));
        assertEquals("Hammersbach", service.getNearByFor(11.06561, 47.42428));
        assertEquals("Earth", service.getNearByFor(0.0, 0.0));
        // was: assertEquals("Antarctica (general)", service.getNearByFor(0.0, -90.0));
        assertEquals("Amundsen-Scott /USA/", service.getNearByFor(0.0, -90.0));
        assertEquals("North Pole", service.getNearByFor(0.0, 90.0));
        assertEquals(null, service.getNearByFor(90.0, 90.0));
        assertEquals(null, service.getNearByFor(-90.0, -90.0));
    }

    public void testNearByPlaceNameLookup() throws IOException {
        assertEquals("Atzmännig", service.getNearByPlaceNameFor(9.0, 47.3));
        assertEquals("Grafenrheinfeld", service.getNearByPlaceNameFor(10.2, 50.001));
        assertEquals("Hammersbach", service.getNearByPlaceNameFor(11.06561, 47.42428));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, 0.0));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, -90.0));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, 90.0));
        assertEquals(null, service.getNearByPlaceNameFor(90.0, 90.0));
        assertEquals(null, service.getNearByPlaceNameFor(-90.0, -90.0));
    }
}
