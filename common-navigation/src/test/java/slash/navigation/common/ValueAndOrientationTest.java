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
 * Tests for {@link ValueAndOrientation} ? value/orientation record, equals/hashCode/toString.
 *
 * @author Christian Pesch
 */
public class ValueAndOrientationTest {

    @Test
    public void testConstructorAndAccessors() {
        ValueAndOrientation vo = new ValueAndOrientation(12.345, Orientation.East);
        assertEquals(12.345, vo.value(), 0.0);
        assertEquals(Orientation.East, vo.orientation());
    }

    @Test
    public void testEqualsSelf() {
        ValueAndOrientation vo = new ValueAndOrientation(1.0, Orientation.North);
        assertEquals(vo, vo);
    }

    @Test
    public void testEqualsSymmetric() {
        ValueAndOrientation a = new ValueAndOrientation(48.5, Orientation.North);
        ValueAndOrientation b = new ValueAndOrientation(48.5, Orientation.North);
        assertEquals(a, b);
        assertEquals(b, a);
    }

    @Test
    public void testNotEqualsDifferentValue() {
        ValueAndOrientation a = new ValueAndOrientation(48.5, Orientation.North);
        ValueAndOrientation b = new ValueAndOrientation(49.0, Orientation.North);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualsDifferentOrientation() {
        ValueAndOrientation a = new ValueAndOrientation(48.5, Orientation.North);
        ValueAndOrientation b = new ValueAndOrientation(48.5, Orientation.South);
        assertNotEquals(a, b);
    }

    @Test
    public void testNotEqualsNull() {
        ValueAndOrientation vo = new ValueAndOrientation(1.0, Orientation.East);
        assertNotEquals(vo, null);
    }

    @Test
    public void testNotEqualsDifferentType() {
        ValueAndOrientation vo = new ValueAndOrientation(1.0, Orientation.East);
        assertNotEquals(vo, "string");
    }

    @Test
    public void testHashCodeConsistency() {
        ValueAndOrientation vo = new ValueAndOrientation(10.0, Orientation.West);
        assertEquals(vo.hashCode(), vo.hashCode());
    }

    @Test
    public void testHashCodeEqualObjects() {
        ValueAndOrientation a = new ValueAndOrientation(10.0, Orientation.West);
        ValueAndOrientation b = new ValueAndOrientation(10.0, Orientation.West);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToStringContainsValueAndOrientation() {
        ValueAndOrientation vo = new ValueAndOrientation(42.0, Orientation.South);
        String s = vo.toString();
        assertTrue(s.contains("42.0"));
        assertTrue(s.contains("South"));
    }

    @Test
    public void testZeroValueWithNorthOrientation() {
        ValueAndOrientation vo = new ValueAndOrientation(0.0, Orientation.North);
        assertEquals(0.0, vo.value(), 0.0);
        assertEquals(Orientation.North, vo.orientation());
    }
}

