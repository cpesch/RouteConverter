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

import static org.junit.Assert.*;

/**
 * Tests for {@link DistanceAndTime} ? value record, ZERO constant, equals/hashCode/toString.
 *
 * @author Christian Pesch
 */
public class DistanceAndTimeTest {

    @Test
    public void testZeroConstant() {
        assertNotNull(DistanceAndTime.ZERO);
        assertEquals(0.0, DistanceAndTime.ZERO.distance(), 0.0);
        assertEquals(0L, (long) DistanceAndTime.ZERO.timeInMillis());
    }

    @Test
    public void testConstructorAndAccessors() {
        DistanceAndTime dt = new DistanceAndTime(1500.0, 60_000L);
        assertEquals(1500.0, dt.distance(), 0.0);
        assertEquals(60_000L, (long) dt.timeInMillis());
    }

    @Test
    public void testConstructorWithNullValues() {
        DistanceAndTime dt = new DistanceAndTime(null, null);
        assertNull(dt.distance());
        assertNull(dt.timeInMillis());
    }

    @Test
    public void testEqualsSelf() {
        DistanceAndTime dt = new DistanceAndTime(100.0, 5000L);
        assertEquals(dt, dt);
    }

    @Test
    public void testEqualsSymmetric() {
        DistanceAndTime a = new DistanceAndTime(100.0, 5000L);
        DistanceAndTime b = new DistanceAndTime(100.0, 5000L);
        assertEquals(a, b);
        assertEquals(b, a);
    }

    @Test
    public void testEqualsBothNulls() {
        DistanceAndTime a = new DistanceAndTime(null, null);
        DistanceAndTime b = new DistanceAndTime(null, null);
        assertEquals(a, b);
    }

    @Test
    public void testNotEqualsDifferentDistance() {
        DistanceAndTime a = new DistanceAndTime(100.0, 5000L);
        DistanceAndTime b = new DistanceAndTime(200.0, 5000L);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualsDifferentTime() {
        DistanceAndTime a = new DistanceAndTime(100.0, 5000L);
        DistanceAndTime b = new DistanceAndTime(100.0, 6000L);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualsNull() {
        DistanceAndTime dt = new DistanceAndTime(1.0, 1L);
        assertNotEquals(dt, null);
    }

    @Test
    public void testNotEqualsDifferentType() {
        DistanceAndTime dt = new DistanceAndTime(1.0, 1L);
        assertNotEquals(dt, "string");
    }

    @Test
    public void testHashCodeConsistency() {
        DistanceAndTime dt = new DistanceAndTime(250.0, 12_000L);
        assertEquals(dt.hashCode(), dt.hashCode());
    }

    @Test
    public void testHashCodeEqualObjects() {
        DistanceAndTime a = new DistanceAndTime(250.0, 12_000L);
        DistanceAndTime b = new DistanceAndTime(250.0, 12_000L);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testHashCodeNullValues() {
        DistanceAndTime dt = new DistanceAndTime(null, null);
        // should not throw
        dt.hashCode();
    }

    @Test
    public void testToStringContainsValues() {
        DistanceAndTime dt = new DistanceAndTime(999.0, 3600_000L);
        String s = dt.toString();
        assertTrue(s.contains("999.0"));
        assertTrue(s.contains("3600000"));
    }
}

