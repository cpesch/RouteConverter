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
package slash.navigation.photon;

import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PhotonServiceIT {
    private PhotonService service = new PhotonService();

    @Test
    public void getPositionsFor() throws IOException {
        List<SimpleNavigationPosition> expected = asList(
                new SimpleNavigationPosition(10.1994416, 50.0002652, null, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Bavaria, Germany (highway)"),
                new SimpleNavigationPosition(10.200847241161895, 50.0011614, null, "Pfarrheim, 97506 Grafenrheinfeld, Bavaria, Germany (building)"),
                new SimpleNavigationPosition(10.2004901, 50.0014652, null, "Kindertagesst\u00e4tte St. Joseph, 97506 Grafenrheinfeld, Bavaria, Germany (amenity)")
        );
        List<NavigationPosition> actual = service.getPositionsFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }

    @Test
    public void getAddressFor() throws IOException {
        assertEquals("Isarenloch, 8733 Eschenbach (SG), Sankt Gallen, Switzerland", service.getAddressFor(new SimpleNavigationPosition(9.0, 47.3)));
        assertEquals("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Bavaria, Germany", service.getAddressFor(new SimpleNavigationPosition(10.2, 50.001)));
        assertEquals("Hoher Gaif, Garmisch-Partenkirchen, Bavaria, Germany", service.getAddressFor(new SimpleNavigationPosition(11.06561, 47.42428)));
        assertEquals("Atlas Buoy 0.00E 0.00N", service.getAddressFor(new SimpleNavigationPosition(0.0, 0.0)));
        assertEquals("North Pole", service.getAddressFor(new SimpleNavigationPosition(0.0, 90.0)));
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(0.0, -90.0)).contains("South Pole"));
        assertEquals(null, service.getAddressFor(new SimpleNavigationPosition(-90.0, 0.0)));
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(-90.0, -90.0)).contains("South Pole"));
        assertEquals("North Pole", service.getAddressFor(new SimpleNavigationPosition(90.0, 90.0)));
    }
}
