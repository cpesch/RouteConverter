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

import org.junit.jupiter.api.Test;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhotonServiceIT {
    private final PhotonService service = new PhotonService();

    private void assertPositionsAlmostEqual(List<SimpleCategorizedNavigationPosition> expected, List<GeocodingResult> actual, double tolerance) {
        assertEquals(expected.size(), actual.size(), "List sizes differ");
        for (int i = 0; i < expected.size(); i++) {
            SimpleCategorizedNavigationPosition exp = expected.get(i);
            GeocodingResult result = actual.get(i);
            assertEquals(service.getName(), result.getGeocodingServiceName(), "Service name differs at index " + i);
            CategorizedNavigationPosition act = result.getPosition();
            assertEquals(exp.getLatitude(), act.getLatitude(), tolerance, "Latitude differs at index " + i);
            assertEquals(exp.getLongitude(), act.getLongitude(), tolerance, "Longitude differs at index " + i);
            assertEquals(exp.getDescription(), act.getDescription(), "Description differs at index " + i);
            assertEquals(exp.getCategory(), act.getCategory(), "Category differs at index " + i);
        }
    }

    @Test
    public void getPositionsFor() throws IOException {
        List<SimpleCategorizedNavigationPosition> expected = asList(
                new SimpleCategorizedNavigationPosition(10.2006845, 50.0004970, null, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Bayern, Deutschland", "highway"),
                new SimpleCategorizedNavigationPosition(10.2001313, 50.0016142, null, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Bayern, Deutschland", "highway"),
                new SimpleCategorizedNavigationPosition(10.1999005, 50.0001319, null, "B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Bayern, Deutschland", "highway")
        );
        List<GeocodingResult> actual = service.getPositionsFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertPositionsAlmostEqual(expected, actual, 0.01);
    }

    @Test
    public void getAddressFor() throws IOException {
        String address1 = service.getAddressFor(new SimpleNavigationPosition(9.0, 47.3));
        assertTrue(address1.contains("Gallen") && address1.contains("Suisse"));
        String address2 = service.getAddressFor(new SimpleNavigationPosition(10.2, 50.001));
        assertTrue(address2.contains("Grafenrheinfeld") && address2.contains("Bayern") && address2.contains("Deutschland"));
        String address3 = service.getAddressFor(new SimpleNavigationPosition(11.06561, 47.42428));
        assertTrue(address3.contains("Hoher Gaif") && address3.contains("Deutschland"));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(0.0, 0.0)));
        assertEquals("North Pole", service.getAddressFor(new SimpleNavigationPosition(0.0, 90.0)));
        String pole1 = service.getAddressFor(new SimpleNavigationPosition(0.0, -90.0));
        assertTrue(pole1.contains("Pole") || pole1.contains("Skiway"));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(-90.0, 0.0)));
        String pole2 = service.getAddressFor(new SimpleNavigationPosition(-90.0, -90.0));
        assertTrue(pole2.contains("Pole") || pole2.contains("Skiway"));
        assertEquals("North Pole", service.getAddressFor(new SimpleNavigationPosition(90.0, 90.0)));
    }
}
