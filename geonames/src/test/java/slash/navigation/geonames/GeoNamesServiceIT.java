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

import slash.common.TestCase;

import java.io.IOException;

public class GeoNamesServiceIT extends TestCase {
    private GeoNamesService service = new GeoNamesService();

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
        // was: assertEquals(null, service.getNearByFor(0.0, 0.0));
        assertEquals("Earth", service.getNearByFor(0.0, 0.0));
        String southPole = service.getNearByFor(0.0, -90.0);
        assertTrue("South Pole".equals(southPole) || "Antarctica".equals(southPole));
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
        assertEquals(new PostalCode("CH", "9622", "Krinau"), service.getNearByPostalCodeFor(9.0, 47.3));
        assertEquals(new PostalCode("DE", "97506", "Grafenrheinfeld"), service.getNearByPostalCodeFor(10.2, 50.001));
        assertEquals(new PostalCode("AT", "6105", "Obern"), service.getNearByPostalCodeFor(11.06561, 47.42428));
        assertEquals(null, service.getNearByPostalCodeFor(0.0, 0.0));
        assertEquals(null, service.getNearByPostalCodeFor(0.0, -90.0));
        assertEquals(new PostalCode("CA", "H0H", "Reserved (Santa Claus)"), service.getNearByPostalCodeFor(0.0, 90.0));
        assertEquals(null, service.getNearByPostalCodeFor(90.0, 90.0));
        assertEquals(null, service.getNearByPostalCodeFor(-90.0, -90.0));
    }

    public void testPlaceNameFor() throws IOException {
        assertEquals("Krinau", service.getPlaceNameFor("CH", "9622"));
        assertEquals("Grafenrheinfeld", service.getPlaceNameFor("DE", "97506"));
        assertEquals("Leutasch", service.getPlaceNameFor("AT", "6105"));
        assertEquals("Walldorf", service.getPlaceNameFor("DE", "69190"));
        assertEquals("Walldorf", service.getPlaceNameFor("de", "69190"));
    }

    public void testPositionFor() throws IOException {
        assertDoubleArrayEquals(new double[]{9.05, 47.31667}, service.getPositionFor("CH", "9622"));
        assertDoubleArrayEquals(new double[]{10.1982, 50.0002}, service.getPositionFor("DE", "97506"));
        assertDoubleArrayEquals(new double[]{11.1603, 47.3694}, service.getPositionFor("AT", "6105"));
        assertDoubleArrayEquals(new double[]{8.64415, 49.30075}, service.getPositionFor("DE", "69190"));
        assertDoubleArrayEquals(new double[]{8.64415, 49.30075}, service.getPositionFor("de", "69190"));
    }
}
