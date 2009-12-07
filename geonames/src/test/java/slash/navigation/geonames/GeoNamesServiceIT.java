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
package slash.navigation.geonames;

import junit.framework.TestCase;

import java.io.IOException;

public class GeoNamesServiceIT extends TestCase {
    private final GeoNamesService service = new GeoNamesService();

    public void testSrtm3ElevationFor() throws IOException {
        assertEquals(209, service.getSrtm3ElevationFor(10.2, 50.001).intValue());
        assertEquals(null, service.getSrtm3ElevationFor(11.06561, 47.42428));
        assertEquals(null, service.getSrtm3ElevationFor(0.0, 0.0));

        assertEquals(40, service.getSrtm3ElevationFor(11.2, 59.0).intValue());
        assertEquals(190, service.getSrtm3ElevationFor(11.2, 60.0).intValue());
        assertEquals(null, service.getSrtm3ElevationFor(11.2, 61.0));

        assertEquals(77, service.getSrtm3ElevationFor(-68.0, -54.0).intValue());
        assertEquals(455, service.getSrtm3ElevationFor(-68.0, -55.0).intValue());
        assertEquals(0, service.getSrtm3ElevationFor(-68.0, -56.0).intValue());
        assertEquals(null, service.getSrtm3ElevationFor(-68.0, -56.1));
        assertEquals(null, service.getSrtm3ElevationFor(-68.0, -57.0));
    }

    public void testGtopo30ElevationFor() throws IOException {
        assertEquals(205, service.getGtopo30ElevationFor(10.2, 50.001).intValue());
        assertEquals(1789, service.getGtopo30ElevationFor(11.06561, 47.42428).intValue());
        assertEquals(null, service.getGtopo30ElevationFor(0.0, 0.0));

        assertEquals(17, service.getGtopo30ElevationFor(11.2, 59.0).intValue());
        assertEquals(120, service.getGtopo30ElevationFor(11.2, 60.0).intValue());
        assertEquals(648, service.getGtopo30ElevationFor(11.2, 61.0).intValue());

        assertEquals(119, service.getGtopo30ElevationFor(-68.0, -54.0).intValue());
        assertEquals(184, service.getGtopo30ElevationFor(-68.0, -55.0).intValue());
        assertEquals(null, service.getGtopo30ElevationFor(-68.0, -56.0));
        assertEquals(null, service.getGtopo30ElevationFor(-68.0, -56.1));
        assertEquals(null, service.getGtopo30ElevationFor(-68.0, -57.0));
    }

    public void testElevationFor() throws IOException {
        assertEquals(40, service.getElevationFor(11.2, 59.0).intValue());
        assertEquals(120, service.getElevationFor(11.2, 60.0).intValue());
        assertEquals(648, service.getElevationFor(11.2, 61.0).intValue());

        assertEquals(77, service.getElevationFor(-68.0, -54.0).intValue());
        assertEquals(455, service.getElevationFor(-68.0, -55.0).intValue());
        assertEquals(null, service.getElevationFor(-68.0, -56.0));
        assertEquals(null, service.getElevationFor(-68.0, -56.1));
        assertEquals(null, service.getElevationFor(-68.0, -57.0));
    }

    public void testNearByFor() throws IOException {
        // was: assertEquals("Kreuzegg", service.getNearByFor(9.0, 47.3));
        assertEquals("Atzmännig", service.getNearByFor(9.0, 47.3));
        assertEquals("Grafenrheinfeld", service.getNearByFor(10.2, 50.001));
        // was: assertEquals("Hoher Gaif", service.getNearByFor(11.06561, 47.42428));
        assertEquals("Hammersbach", service.getNearByFor(11.06561, 47.42428));
        assertEquals("Earth", service.getNearByFor(0.0, 0.0));
        // was: assertEquals("Antarctica (general)", service.getNearByFor(0.0, -90.0));
        // was: assertEquals("Amundsen-Scott /USA/", service.getNearByFor(0.0, -90.0));
        assertEquals("Antarctica", service.getNearByFor(0.0, -90.0));
        assertEquals("North Pole", service.getNearByFor(0.0, 90.0));
        assertEquals(null, service.getNearByFor(90.0, 90.0));
        assertEquals(null, service.getNearByFor(-90.0, -90.0));
    }

    public void testNearByPlaceNameFor() throws IOException {
        assertEquals("Atzmännig", service.getNearByPlaceNameFor(9.0, 47.3));
        assertEquals("Grafenrheinfeld", service.getNearByPlaceNameFor(10.2, 50.001));
        assertEquals("Hammersbach", service.getNearByPlaceNameFor(11.06561, 47.42428));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, 0.0));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, -90.0));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, 90.0));
        assertEquals(null, service.getNearByPlaceNameFor(90.0, 90.0));
        assertEquals(null, service.getNearByPlaceNameFor(-90.0, -90.0));
    }

    public void testNearByPostalCodeFor() throws IOException {
        assertEquals("CH 9622", service.getNearByPostalCodeFor(9.0, 47.3));
        assertEquals("DE 97506", service.getNearByPostalCodeFor(10.2, 50.001));
        assertEquals("Hammersbach", service.getNearByPlaceNameFor(11.06561, 47.42428));
        assertEquals(null, service.getNearByPostalCodeFor(0.0, 0.0));
        assertEquals(null, service.getNearByPostalCodeFor(0.0, -90.0));
        assertEquals("CA H0H", service.getNearByPostalCodeFor(0.0, 90.0));
        assertEquals(null, service.getNearByPostalCodeFor(90.0, 90.0));
        assertEquals(null, service.getNearByPostalCodeFor(-90.0, -90.0));
    }
}
