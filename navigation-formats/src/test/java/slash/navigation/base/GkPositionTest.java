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

package slash.navigation.base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests for {@link GkPosition}.
 */
public class GkPositionTest {

    // Munich in WGS84: lon=11.576, lat=48.137
    private static final double MUNICH_LON = 11.576;
    private static final double MUNICH_LAT = 48.137;
    private static final double DELTA = 0.001;

    // --- constructor from lon/lat ---

    @Test
    public void testConstructFromLonLatGetLongitude() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        assertEquals(MUNICH_LON, pos.getLongitude(), DELTA);
    }

    @Test
    public void testConstructFromLonLatGetLatitude() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        assertEquals(MUNICH_LAT, pos.getLatitude(), DELTA);
    }

    @Test
    public void testConstructFromLonLatRightAndHeightArePositive() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        // Gauss-Krueger values are large positive numbers for Central Europe
        assertNotNull(pos.getRight());
        assertNotNull(pos.getHeight());
        assert pos.getRight() > 0 : "Right (easting) should be positive";
        assert pos.getHeight() > 0 : "Height (northing) should be positive";
    }

    @Test
    public void testConstructFromLonLatDescription() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        assertEquals("Munich", pos.getDescription());
    }

    @Test
    public void testConstructFromLonLatElevation() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, 520.0, null, null, "Munich");
        assertEquals(520.0, pos.getElevation(), 0.001);
    }

    // --- constructor from right/height ---

    @Test
    public void testConstructFromRightHeight() {
        GkPosition pos = new GkPosition(4471000.0, 5332000.0, "Somewhere");
        assertEquals(4471000.0, pos.getRight(), 0.1);
        assertEquals(5332000.0, pos.getHeight(), 0.1);
        assertEquals("Somewhere", pos.getDescription());
    }

    @Test
    public void testConstructFromRightHeightGetLonLatRoundtrip() {
        // Build from lon/lat, read right/height, rebuild from right/height, compare lon/lat
        GkPosition original = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, null);
        double right = original.getRight();
        double height = original.getHeight();

        GkPosition rebuilt = new GkPosition(right, height, null);
        assertEquals(MUNICH_LON, rebuilt.getLongitude(), DELTA);
        assertEquals(MUNICH_LAT, rebuilt.getLatitude(), DELTA);
    }

    // --- asGkPosition ---

    @Test
    public void testAsGkPositionReturnsSelf() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, null);
        assertSame(pos, pos.asGkPosition());
    }

    // --- setDescription ---

    @Test
    public void testSetDescription() {
        GkPosition pos = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Old");
        pos.setDescription("New");
        assertEquals("New", pos.getDescription());
    }

    // --- equals / hashCode ---

    @Test
    public void testEqualsAndHashCodeSameValues() {
        GkPosition a = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        GkPosition b = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentDescription() {
        GkPosition a = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Munich");
        GkPosition b = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, "Berlin");
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentCoordinates() {
        GkPosition a = new GkPosition(MUNICH_LON, MUNICH_LAT, null, null, null, null);
        GkPosition b = new GkPosition(13.404, 52.520, null, null, null, null);
        assertNotEquals(a, b);
    }
}

