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

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;

public class GoogleMapsServiceIT {
    private GoogleMapsService service = new GoogleMapsService();

    @Before
    public void setUp() {
        Locale.setDefault(ENGLISH);
    }

    @Test
    public void getLocationFor() throws IOException {
        assertEquals("Chammstrasse 28, 8638 Goldingen, Switzerland", service.getLocationFor(9.0, 47.3));
        assertEquals("B\u00fchlstra\u00dfe 21, 97506 Grafenrheinfeld, Germany", service.getLocationFor(10.2, 50.001));
        assertEquals("Drehm\u00f6ser 1, 82467 Garmisch-Partenkirchen, Germany", service.getLocationFor(11.06561, 47.42428));
        assertEquals(null, service.getLocationFor(0.0, 0.0));
        assertEquals(null, service.getLocationFor(0.0, 90.0));
        assertEquals("Antarctica", service.getLocationFor(0.0, -90.0));
        assertEquals("Galapagos Islands, Ecuador", service.getLocationFor(-90.0, 0.0));
        assertEquals("Antarctica", service.getLocationFor(-90.0, -90.0));
        assertEquals(null, service.getLocationFor(90.0, 90.0));
    }

    @Test
    public void getPositionFor() throws IOException {
        GoogleMapsPosition expected = new GoogleMapsPosition(10.2003632, 50.0004554, 0.0, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        GoogleMapsPosition actual = service.getPositionFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }

    @Test
    public void getPositionsFor() throws IOException {
        GoogleMapsPosition expected = new GoogleMapsPosition(10.2003632, 50.0004554, 0.0, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        List<GoogleMapsPosition> actual = service.getPositionsFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(Arrays.asList(expected), actual);
    }
}