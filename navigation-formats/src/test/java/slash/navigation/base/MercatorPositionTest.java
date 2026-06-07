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
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link MercatorPosition}.
 */
public class MercatorPositionTest {

    // Frankfurt: lon=8.682, lat=50.110
    private static final double FRANKFURT_LON = 8.682;
    private static final double FRANKFURT_LAT = 50.110;
    private static final double DELTA = 0.001;

    // --- constructor from lon/lat ---

    @Test
    public void testConstructFromLonLatGetLongitude() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        assertEquals(FRANKFURT_LON, pos.getLongitude(), DELTA);
    }

    @Test
    public void testConstructFromLonLatGetLatitude() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        assertEquals(FRANKFURT_LAT, pos.getLatitude(), DELTA);
    }

    @Test
    public void testConstructFromLonLatXandYAreNotNull() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        assertNotNull(pos.getX());
        assertNotNull(pos.getY());
    }

    @Test
    public void testConstructFromLonLatDescription() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        assertEquals("Frankfurt", pos.getDescription());
    }

    @Test
    public void testConstructFromLonLatElevation() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, 112.0, null, null, null);
        assertEquals(112.0, pos.getElevation(), 0.001);
    }

    // --- constructor from X/Y ---

    @Test
    public void testConstructFromXYGetXandY() {
        long x = 967000L;
        long y = 6500000L;
        MercatorPosition pos = new MercatorPosition(x, y, null, null, null, "XY");
        assertEquals(x, (long) pos.getX());
        assertEquals(y, (long) pos.getY());
    }

    @Test
    public void testConstructFromXYGetDescription() {
        MercatorPosition pos = new MercatorPosition(967000L, 6500000L, null, null, null, "XY");
        assertEquals("XY", pos.getDescription());
    }

    // --- null lon/lat ---

    @Test
    public void testNullLongitudeAndLatitude() {
        MercatorPosition pos = new MercatorPosition((Double) null, (Double) null, null, null, null, null);
        assertNull(pos.getLongitude());
        assertNull(pos.getLatitude());
        assertNull(pos.getX());
        assertNull(pos.getY());
    }

    // --- round-trip lon/lat ? X/Y ? lon/lat ---

    @Test
    public void testRoundTripLonLatToXYToLonLat() {
        MercatorPosition original = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, null);
        Long x = original.getX();
        Long y = original.getY();

        MercatorPosition rebuilt = new MercatorPosition(x, y, null, null, null, null);
        assertEquals(FRANKFURT_LON, rebuilt.getLongitude(), DELTA);
        assertEquals(FRANKFURT_LAT, rebuilt.getLatitude(), DELTA);
    }

    // --- setLongitude / setLatitude ---

    @Test
    public void testSetLongitudeUpdatesX() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, null);
        pos.setLongitude(13.404); // Berlin
        assertEquals(13.404, pos.getLongitude(), DELTA);
    }

    @Test
    public void testSetLatitudeUpdatesY() {
        MercatorPosition pos = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, null);
        pos.setLatitude(52.520); // Berlin
        assertEquals(52.520, pos.getLatitude(), DELTA);
    }

    // --- equals / hashCode ---

    @Test
    public void testEqualsAndHashCodeSameValues() {
        MercatorPosition a = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        MercatorPosition b = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualDifferentCoordinates() {
        MercatorPosition a = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, null);
        MercatorPosition b = new MercatorPosition(13.404, 52.520, null, null, null, null);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualDifferentDescription() {
        MercatorPosition a = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Frankfurt");
        MercatorPosition b = new MercatorPosition(FRANKFURT_LON, FRANKFURT_LAT, null, null, null, "Other");
        assertNotEquals(a, b);
    }
}

