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
package slash.navigation.googlemaps;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GoogleMapsServiceIT {
    private GoogleMapsService service = new GoogleMapsService();

    @Test
    public void testLocationLookup() throws IOException {
        assertEquals("Kreuzegg, 8638 Goldingen, Schweiz" /*"Chammstrasse 28, 8638 Goldingen, Schweiz"*/, service.getLocationFor(9.0, 47.3));
        assertEquals("B\u00fchlstra\u00dfe 21, 97506 Grafenrheinfeld, Deutschland", service.getLocationFor(10.2, 50.001));
        assertEquals("Drehm\u00f6ser 1, 82467 Garmisch-Partenkirchen, Deutschland", service.getLocationFor(11.06561, 47.42428));
        assertEquals("" /*"Auburn University Montgomery, PO Box 244023, Montgomery, AL 36124-4023, Vereinigte Staaten"*/ /*"North Pole"*/, service.getLocationFor(0.0, 0.0));
        assertTrue(service.getLocationFor(0.0, -90.0).contains("South Pole"));
        assertEquals("North Pole", service.getLocationFor(0.0, 90.0));
        assertEquals("North Pole", service.getLocationFor(90.0, 90.0));
    }

    @Test
    public void testPositionLookup() throws IOException {
        GoogleMapsPosition expected = new GoogleMapsPosition(10.2003632, 50.0004554, 0.0, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        GoogleMapsPosition actual = service.getPositionFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }
}