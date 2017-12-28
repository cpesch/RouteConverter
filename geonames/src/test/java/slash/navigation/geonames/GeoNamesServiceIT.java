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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.common.helpers.APIKeyRegistry;
import slash.navigation.common.SimpleNavigationPosition;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GeoNamesServiceIT {
    private GeoNamesService service = new GeoNamesService();

    @Before
    public void setUp() {
        APIKeyRegistry.getInstance().setAPIKeyPreference("geonames", "routeconverter");
    }

    @After
    public void tearDown() {
        APIKeyRegistry.getInstance().setAPIKeyPreference("geonames", "");
    }

    @Test
    public void testAsterGDEMElevationFor() throws IOException {
        assertEquals(205, service.getAsterGDEMElevationFor(10.2, 50.001), 5);
        assertEquals(2060, service.getAsterGDEMElevationFor(11.06561, 47.42428), 5);
        assertEquals(null, service.getAsterGDEMElevationFor(0.0, 0.0));

        assertEquals(null, service.getAsterGDEMElevationFor(18.0, 82.0));
        assertEquals(null, service.getAsterGDEMElevationFor(28.0, 83.0));
        assertEquals(null, service.getAsterGDEMElevationFor(38.0, 84.0));

        assertEquals(null, service.getAsterGDEMElevationFor(48.0, -63.0));
        assertEquals(null, service.getAsterGDEMElevationFor(58.0, -64.0));
        assertEquals(null, service.getAsterGDEMElevationFor(68.0, -65.0));
        assertEquals(null, service.getAsterGDEMElevationFor(78.0, -65.1));
        assertEquals(null, service.getAsterGDEMElevationFor(88.0, -66.0));
    }

    @Test
    public void testSrtm3ElevationFor() throws IOException {
        assertEquals(209, service.getSRTM3ElevationFor(10.2, 50.001), 5);
        assertEquals(2071, service.getSRTM3ElevationFor(11.06561, 47.42428), 5);
        assertEquals(null, service.getSRTM3ElevationFor(0.0, 0.0));

        assertEquals(40, service.getSRTM3ElevationFor(11.2, 59.0), 5);
        assertEquals(190, service.getSRTM3ElevationFor(11.2, 60.0), 5);
        assertEquals(null, service.getSRTM3ElevationFor(11.2, 61.0));

        assertEquals(77, service.getSRTM3ElevationFor(-68.0, -54.0), 5);
        assertEquals(455, service.getSRTM3ElevationFor(-68.0, -55.0), 5);
        assertEquals(null, service.getSRTM3ElevationFor(-68.0, -56.0));
        assertEquals(null, service.getSRTM3ElevationFor(-68.0, -56.1));
        assertEquals(null, service.getSRTM3ElevationFor(-68.0, -57.0));
    }

    @Test
    public void testGtopo30ElevationFor() throws IOException {
        assertEquals(205, service.getGTOPO30ElevationFor(10.2, 50.001), 5);
        assertEquals(1789, service.getGTOPO30ElevationFor(11.06561, 47.42428), 5);
        assertEquals(null, service.getGTOPO30ElevationFor(0.0, 0.0));

        assertEquals(17, service.getGTOPO30ElevationFor(11.2, 59.0), 5);
        assertEquals(120, service.getGTOPO30ElevationFor(11.2, 60.0), 5);
        assertEquals(648, service.getGTOPO30ElevationFor(11.2, 61.0), 5);

        assertEquals(119, service.getGTOPO30ElevationFor(-68.0, -54.0), 5);
        assertEquals(184, service.getGTOPO30ElevationFor(-68.0, -55.0), 5);
        assertEquals(null, service.getGTOPO30ElevationFor(-68.0, -56.0));
        assertEquals(null, service.getGTOPO30ElevationFor(-68.0, -56.1));
        assertEquals(null, service.getGTOPO30ElevationFor(-68.0, -57.0));
    }

    @Test
    public void testElevationFor() throws IOException {
        assertEquals(37, service.getElevationFor(11.2, 59.0), 5);
        assertEquals(165, service.getElevationFor(11.2, 60.0), 5);
        assertEquals(656, service.getElevationFor(11.2, 61.0), 5);

        assertEquals(63, service.getElevationFor(-68.0, -54.0), 5);
        assertEquals(460, service.getElevationFor(-68.0, -55.0), 5);
        assertEquals(0, service.getElevationFor(-68.0, -56.0), 5);
        assertEquals(null, service.getElevationFor(-68.0, -56.1));
        assertEquals(null, service.getElevationFor(-68.0, -57.0));
    }

    @Test
    public void testAddressFor() throws IOException {
        assertEquals("St. Margarethen", service.getAddressFor(new SimpleNavigationPosition(9.0, 47.5)));
        assertEquals("Grafenrheinfeld", service.getAddressFor(new SimpleNavigationPosition(10.2, 50.001)));
        assertEquals("Hammersbach", service.getAddressFor(new SimpleNavigationPosition(11.06561, 47.42428)));
        assertEquals("Earth", service.getAddressFor(new SimpleNavigationPosition(0.0, 0.0)));
        assertNotNull(service.getAddressFor(new SimpleNavigationPosition(0.0, -90.0)));
        assertEquals("North Pole", service.getAddressFor(new SimpleNavigationPosition(0.0, 90.0)));
        assertEquals(null, service.getAddressFor(new SimpleNavigationPosition(90.0, 90.0)));
        assertEquals(null, service.getAddressFor(new SimpleNavigationPosition(-90.0, -90.0)));
    }

    @Test
    public void testNearByToponymFor() throws IOException {
        assertEquals("Vogelh\u00e4rd", service.getNearByToponymFor(9.0, 47.5));
        assertEquals("Grafenrheinfeld", service.getNearByToponymFor(10.2, 50.001));
        assertEquals("Hoher Gaif", service.getNearByToponymFor(11.06561, 47.42428));
        assertEquals("Earth", service.getNearByToponymFor(0.0, 0.0));
        assertEquals("South Pole", service.getNearByToponymFor(0.0, -90.0));
        assertEquals("North Pole", service.getNearByToponymFor(0.0, 90.0));
        assertEquals(null, service.getNearByToponymFor(90.0, 90.0));
        assertEquals(null, service.getNearByToponymFor(-90.0, -90.0));
    }

    @Test
    public void testNearByPlaceNameFor() throws IOException {
        assertEquals("St. Margarethen", service.getNearByPlaceNameFor(9.0, 47.5));
        assertEquals("Grafenrheinfeld", service.getNearByPlaceNameFor(10.2, 50.001));
        assertEquals("Hammersbach", service.getNearByPlaceNameFor(11.06561, 47.42428));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, 0.0));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, -90.0));
        assertEquals(null, service.getNearByPlaceNameFor(0.0, 90.0));
        assertEquals(null, service.getNearByPlaceNameFor(90.0, 90.0));
        assertEquals(null, service.getNearByPlaceNameFor(-90.0, -90.0));
    }
}
