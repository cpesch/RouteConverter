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

package slash.navigation.routing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TravelModeTest {

    @Test
    public void testName() {
        TravelMode mode = new TravelMode("Car");
        assertEquals("Car", mode.name());
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        TravelMode a = new TravelMode("Bicycle");
        TravelMode b = new TravelMode("Bicycle");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentName() {
        TravelMode car = new TravelMode("Car");
        TravelMode bike = new TravelMode("Bicycle");
        assertNotEquals(car, bike);
    }

    @Test
    public void testToString() {
        TravelMode mode = new TravelMode("Pedestrian");
        // Record toString includes field names
        String s = mode.toString();
        assert s.contains("Pedestrian") : "toString should contain name value";
    }
}

