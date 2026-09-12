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
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.*;

public class GoogleGeocodingServiceIT {
    private final GoogleGeocodingService service = new GoogleGeocodingService();

    @Before
    public void setUp() {
        Locale.setDefault(ENGLISH);
    }

    @Test
    public void getAddressFor() throws IOException {
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(9.0, 47.3)).contains("Goldingen"));
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(10.2, 50.001)).contains("Grafenrheinfeld"));
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(11.06561, 47.42428)).contains("Garmisch-Partenkirchen"));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(0.0, 0.0)));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(0.0, 90.0)));
        assertNotNull(service.getAddressFor(new SimpleNavigationPosition(0.0, -90.0)));
        assertEquals("Ecuador", service.getAddressFor(new SimpleNavigationPosition(-90.0, 0.0)));
        assertNotNull(service.getAddressFor(new SimpleNavigationPosition(-90.0, -90.0)));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(90.0, 90.0)));
    }

    @Test
    public void getPositionsFor() throws IOException {
        List<GeocodingResult> expected = singletonList(
                new GeocodingResult(new SimpleCategorizedNavigationPosition(10.2004268, 50.0010792, null, "Bühlstraße, 97506 Grafenrheinfeld, Germany", "route"), service.getName())
        );
        List<GeocodingResult> actual = service.getPositionsFor("Bühlstraße, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }
}
