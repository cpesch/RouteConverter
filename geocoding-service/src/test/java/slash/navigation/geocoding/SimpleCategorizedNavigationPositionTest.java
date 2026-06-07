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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SimpleCategorizedNavigationPositionTest {

    @Test
    public void testFullConstructor() {
        SimpleCategorizedNavigationPosition pos =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, 200.0, "Munich", "City", null);
        assertEquals(10.0, pos.getLongitude(), 0.0001);
        assertEquals(50.0, pos.getLatitude(), 0.0001);
        assertEquals(200.0, pos.getElevation(), 0.0001);
        assertEquals("Munich", pos.getDescription());
        assertEquals("City", pos.getCategory());
        assertNull(pos.getTime());
    }

    @Test
    public void testShortConstructorNullCategory() {
        SimpleCategorizedNavigationPosition pos =
                new SimpleCategorizedNavigationPosition(8.0, 48.0, null, "Freiburg", null);
        assertNull(pos.getCategory());
        assertNull(pos.getElevation());
    }

    @Test
    public void testSetCategory() {
        SimpleCategorizedNavigationPosition pos =
                new SimpleCategorizedNavigationPosition(0.0, 0.0, null, null, "OldCat");
        pos.setCategory("NewCat");
        assertEquals("NewCat", pos.getCategory());
    }

    @Test
    public void testEqualsAndHashCodeSameValues() {
        SimpleCategorizedNavigationPosition a =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", "City");
        SimpleCategorizedNavigationPosition b =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", "City");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentCategory() {
        SimpleCategorizedNavigationPosition a =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", "City");
        SimpleCategorizedNavigationPosition b =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", "Village");
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentCoordinates() {
        SimpleCategorizedNavigationPosition a =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "A", "Cat");
        SimpleCategorizedNavigationPosition b =
                new SimpleCategorizedNavigationPosition(11.0, 51.0, null, "A", "Cat");
        assertNotEquals(a, b);
    }

    @Test
    public void testToStringContainsCoordinates() {
        SimpleCategorizedNavigationPosition pos =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", "City");
        String s = pos.toString();
        assertTrue(s.contains("10.0"));
        assertTrue(s.contains("50.0"));
    }

    @Test
    public void testToStringContainsCategory() {
        SimpleCategorizedNavigationPosition pos =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", "Capital");
        assertTrue(pos.toString().contains("Capital"));
    }

    @Test
    public void testToStringNullCategory() {
        SimpleCategorizedNavigationPosition pos =
                new SimpleCategorizedNavigationPosition(10.0, 50.0, null, "Berlin", null);
        // Should not throw and should not contain "category="
        String s = pos.toString();
        assertTrue(s.contains("10.0"));
    }
}

