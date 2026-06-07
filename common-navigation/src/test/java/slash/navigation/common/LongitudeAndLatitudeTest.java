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

package slash.navigation.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LongitudeAndLatitudeTest {

    @Test
    public void testAccessors() {
        LongitudeAndLatitude ll = new LongitudeAndLatitude(10.5, 48.3);
        assertEquals(10.5, ll.longitude(), 0.0001);
        assertEquals(48.3, ll.latitude(), 0.0001);
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        LongitudeAndLatitude a = new LongitudeAndLatitude(10.5, 48.3);
        LongitudeAndLatitude b = new LongitudeAndLatitude(10.5, 48.3);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentLongitude() {
        LongitudeAndLatitude a = new LongitudeAndLatitude(10.5, 48.3);
        LongitudeAndLatitude b = new LongitudeAndLatitude(10.6, 48.3);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentLatitude() {
        LongitudeAndLatitude a = new LongitudeAndLatitude(10.5, 48.3);
        LongitudeAndLatitude b = new LongitudeAndLatitude(10.5, 48.4);
        assertNotEquals(a, b);
    }

    @Test
    public void testZeroCoordinates() {
        LongitudeAndLatitude zero = new LongitudeAndLatitude(0.0, 0.0);
        assertEquals(0.0, zero.longitude(), 0.0);
        assertEquals(0.0, zero.latitude(), 0.0);
    }

    @Test
    public void testNegativeCoordinates() {
        LongitudeAndLatitude ll = new LongitudeAndLatitude(-73.9857, -33.8688);
        assertEquals(-73.9857, ll.longitude(), 0.0001);
        assertEquals(-33.8688, ll.latitude(), 0.0001);
    }
}

