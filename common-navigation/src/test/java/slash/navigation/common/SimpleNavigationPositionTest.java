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
 * Tests for {@link SimpleNavigationPosition} ? constructor variants, accessors, equals/hashCode/toString.
 *
 * @author Christian Pesch
 */
public class SimpleNavigationPositionTest {

    @Test
    public void testConstructorFull() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0, 100.0, "Berlin", null);
        assertEquals(10.0, p.getLongitude(), 0.0);
        assertEquals(50.0, p.getLatitude(), 0.0);
        assertEquals(100.0, p.getElevation(), 0.0);
        assertEquals("Berlin", p.getDescription());
        assertNull(p.getTime());
    }

    @Test
    public void testConstructorWithoutTime() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(8.0, 48.0, 200.0, "Munich");
        assertEquals(8.0, p.getLongitude(), 0.0);
        assertEquals(48.0, p.getLatitude(), 0.0);
        assertEquals(200.0, p.getElevation(), 0.0);
        assertEquals("Munich", p.getDescription());
        assertNull(p.getTime());
    }

    @Test
    public void testConstructorWithOnlyLongLatTime() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(7.0, 47.0, null);
        assertEquals(7.0, p.getLongitude(), 0.0);
        assertEquals(47.0, p.getLatitude(), 0.0);
        assertNull(p.getElevation());
        assertNull(p.getTime());
    }

    @Test
    public void testConstructorMinimal() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(1.0, 2.0);
        assertEquals(1.0, p.getLongitude(), 0.0);
        assertEquals(2.0, p.getLatitude(), 0.0);
        assertNull(p.getElevation());
        assertNull(p.getDescription());
        assertNull(p.getTime());
    }

    @Test
    public void testHasCoordinatesTrue() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0);
        assertTrue(p.hasCoordinates());
    }

    @Test
    public void testHasCoordinatesFalseNullLon() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(null, 50.0, null, null);
        assertFalse(p.hasCoordinates());
    }

    @Test
    public void testHasCoordinatesFalseNullLat() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, null, null, null);
        assertFalse(p.hasCoordinates());
    }

    @Test
    public void testHasTimeFalse() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0);
        assertFalse(p.hasTime());
    }

    @Test
    public void testSettersAndGetters() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(1.0, 2.0);
        p.setLongitude(11.0);
        p.setLatitude(51.0);
        p.setElevation(300.0);
        p.setDescription("Hamburg");
        p.setSpeed(60.0);
        assertEquals(11.0, p.getLongitude(), 0.0);
        assertEquals(51.0, p.getLatitude(), 0.0);
        assertEquals(300.0, p.getElevation(), 0.0);
        assertEquals("Hamburg", p.getDescription());
        assertEquals(60.0, p.getSpeed(), 0.0);
    }

    @Test
    public void testEqualsAndHashCodeSelf() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0, 100.0, "X");
        assertEquals(p, p);
        assertEquals(p.hashCode(), p.hashCode());
    }

    @Test
    public void testEqualsSymmetric() {
        SimpleNavigationPosition a = new SimpleNavigationPosition(10.0, 50.0, 100.0, "X");
        SimpleNavigationPosition b = new SimpleNavigationPosition(10.0, 50.0, 100.0, "X");
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualsDifferentLon() {
        SimpleNavigationPosition a = new SimpleNavigationPosition(10.0, 50.0, 100.0, "X");
        SimpleNavigationPosition b = new SimpleNavigationPosition(11.0, 50.0, 100.0, "X");
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualsNull() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0);
        assertNotEquals(p, null);
    }

    @Test
    public void testNotEqualsDifferentType() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0);
        assertNotEquals(p, "not a position");
    }

    @Test
    public void testToStringContainsCoordinates() {
        SimpleNavigationPosition p = new SimpleNavigationPosition(10.0, 50.0, 100.0, "Paris");
        String s = p.toString();
        assertTrue(s.contains("10.0"));
        assertTrue(s.contains("50.0"));
        assertTrue(s.contains("Paris"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCalculateDistanceThrows() {
        new SimpleNavigationPosition(10.0, 50.0).calculateDistance(new SimpleNavigationPosition(11.0, 51.0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCalculateAngleThrows() {
        new SimpleNavigationPosition(10.0, 50.0).calculateAngle(new SimpleNavigationPosition(11.0, 51.0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCalculateElevationThrows() {
        new SimpleNavigationPosition(10.0, 50.0).calculateElevation(new SimpleNavigationPosition(11.0, 51.0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCalculateTimeThrows() {
        new SimpleNavigationPosition(10.0, 50.0).calculateTime(new SimpleNavigationPosition(11.0, 51.0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCalculateSpeedThrows() {
        new SimpleNavigationPosition(10.0, 50.0).calculateSpeed(new SimpleNavigationPosition(11.0, 51.0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetStartDateThrows() {
        new SimpleNavigationPosition(10.0, 50.0).setStartDate(null);
    }
}

