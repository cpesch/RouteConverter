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
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        assertTrue(service.getLocationFor(11.06561, 47.42428).endsWith("82467 Garmisch-Partenkirchen, Germany"));
        assertEquals(null, service.getLocationFor(0.0, 0.0));
        assertEquals(null, service.getLocationFor(0.0, 90.0));
        assertTrue(service.getLocationFor(0.0, -90.0).contains("Antarctica"));
        assertEquals("Ecuador", service.getLocationFor(-90.0, 0.0));
        assertTrue(service.getLocationFor(-90.0, -90.0).contains("Antarctica"));
        assertEquals(null, service.getLocationFor(90.0, 90.0));
    }

    @Test
    public void getPositionFor() throws IOException {
        NavigationPosition expected = new SimpleNavigationPosition(10.2004684, 50.0010183, 0.0, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        NavigationPosition actual = service.getPositionFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }

    @Test
    public void getPositionsFor() throws IOException {
        NavigationPosition expected = new SimpleNavigationPosition(10.2004684, 50.0010183, 0.0, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        List<NavigationPosition> actual = service.getPositionsFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(singletonList(expected), actual);
    }

    @Test
    public void getElevationFor() throws IOException {
        assertEquals(39.3, service.getElevationFor(11.2, 59.0), 0.5);
        assertEquals(185.6, service.getElevationFor(11.2, 60.0), 0.5);
        assertEquals(650.1, service.getElevationFor(11.2, 61.0), 0.5);

        assertEquals(77.2, service.getElevationFor(-68.0, -54.0), 0.5);
        assertEquals(457.6, service.getElevationFor(-68.0, -55.0), 0.5);
        assertEquals(-110.4970398, service.getElevationFor(-68.0, -56.0), 0.5);
        assertEquals(-105.0, service.getElevationFor(-68.0, -56.1), 0.5);
        assertEquals(-2893.809082, service.getElevationFor(-68.0, -57.0), 0.5);
    }
}