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

import junit.framework.TestCase;

import java.io.IOException;

public class GoogleMapsServiceIT extends TestCase {
    private GoogleMapsService service = new GoogleMapsService();

    public void testLocationLookup() throws IOException {
        assertEquals("8638 Goldingen, Schweiz", service.getLocationFor(9.0, 47.3));
        assertEquals("Bühlstraße 21, 97506 Grafenrheinfeld, Deutschland", service.getLocationFor(10.2, 50.001));
        assertEquals("82467 Garmisch-Partenkirchen, Deutschland", service.getLocationFor(11.06561, 47.42428));
        assertEquals("North Atlantic Ocean", service.getLocationFor(0.0, 0.0));
        assertEquals("Antarktis", service.getLocationFor(0.0, -90.0));
        assertEquals("North Pole", service.getLocationFor(0.0, 90.0));
        assertEquals("North Pole", service.getLocationFor(90.0, 90.0));
    }

    public void testPositionLookup() throws IOException {
        GoogleMapsPosition expected = new GoogleMapsPosition(10.2003632, 50.0004554, 0.0, "Bühlstraße, 97506 Grafenrheinfeld, Germany");
        GoogleMapsPosition actual = service.getPositionFor("Bühlstraße, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }
}