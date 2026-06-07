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

package slash.navigation.geocoding;

import org.junit.Test;
import slash.navigation.common.SimpleNavigationPosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GeocodingResultTest {

    private final SimpleCategorizedNavigationPosition position =
            new SimpleCategorizedNavigationPosition(10.0, 50.0, 100.0, "Berlin", "City");
    private final GeocodingResult result = new GeocodingResult(position, "Nominatim");

    @Test
    public void testGetPosition() {
        assertSame(position, result.getPosition());
    }

    @Test
    public void testGetGeocodingServiceName() {
        assertEquals("Nominatim", result.getGeocodingServiceName());
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        GeocodingResult other = new GeocodingResult(position, "Nominatim");
        assertEquals(result, other);
        assertEquals(result.hashCode(), other.hashCode());
    }

    @Test
    public void testNotEqualDifferentServiceName() {
        GeocodingResult other = new GeocodingResult(position, "Photon");
        assertNotEquals(result, other);
    }

    @Test
    public void testNotEqualDifferentPosition() {
        SimpleCategorizedNavigationPosition otherPos =
                new SimpleCategorizedNavigationPosition(11.0, 51.0, null, "Hamburg", null);
        GeocodingResult other = new GeocodingResult(otherPos, "Nominatim");
        assertNotEquals(result, other);
    }

    @Test
    public void testNotEqualNull() {
        assertNotEquals(result, null);
    }

    @Test
    public void testToStringContainsFields() {
        String s = result.toString();
        assertTrue(s.contains("Nominatim"));
    }
}

