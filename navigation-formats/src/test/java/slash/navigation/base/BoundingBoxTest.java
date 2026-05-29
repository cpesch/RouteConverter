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
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;

public class BoundingBoxTest {
    public static NavigationPosition asPosition(double longitude, double latitude) {
        return new SimpleNavigationPosition(longitude, latitude);
    }

    @Test
    public void testContainsPosition() {
        assertTrue(new BoundingBox(0.0, 0.0, 0.0, 0.0).contains(asPosition(0.0, 0.0)));
        assertTrue(new BoundingBox(0.0, 0.0, -0.1, -0.1).contains(asPosition(0.0, 0.0)));
        assertTrue(new BoundingBox(0.0, 0.0, -0.1, -0.1).contains(asPosition(-0.1, -0.1)));
        assertTrue(new BoundingBox(0.1, 0.1, -0.1, -0.1).contains(asPosition(0.0, 0.0)));
        assertTrue(new BoundingBox(-1.0, -1.0, -2.0, -2.0).contains(asPosition(-1.5, -1.5)));
        assertTrue(new BoundingBox(-1.0, 2.0, -2.0, 1.0).contains(asPosition(-1.5, 1.5)));
        assertTrue(new BoundingBox(2.0, 2.0, 1.0, 1.0).contains(asPosition(1.5, 1.5)));
        assertTrue(new BoundingBox(2.0, -1.0, 1.0, -2.0).contains(asPosition(1.5, -1.5)));
    }

    @Test
    public void testNotContainsPosition() {
        assertFalse(new BoundingBox(-0.1, -0.1, 0.0, 0.0).contains(asPosition(0.0, 0.0)));
        assertFalse(new BoundingBox(-0.1, -0.1, -0.1, -0.1).contains(asPosition(0.0, 0.0)));
    }

    @Test
    public void testContainsBoundingBox() {
        assertTrue(new BoundingBox(0.0, 0.0, 0.0, 0.0).contains(new BoundingBox(0.0, 0.0, 0.0, 0.0)));
        assertTrue(new BoundingBox(0.1, 0.1, -0.1, -0.1).contains(new BoundingBox(0.0, 0.0, 0.0, 0.0)));
        assertTrue(new BoundingBox(0.1, 0.1, -0.1, -0.1).contains(new BoundingBox(0.1, 0.1, -0.1, -0.1)));
    }

    @Test
    public void testNotContainsBoundingBox() {
        assertFalse(new BoundingBox(0.1, 0.1, -0.1, -0.1).contains(new BoundingBox(0.2, 0.0, 0.0, 0.0)));
    }

    @Test
    public void testGetSquareSize() {
        assertDoubleEquals(16, new BoundingBox(2.0, 2.0, -2.0, -2.0).getSquareSize());
        assertDoubleEquals(16, new BoundingBox(-2.0, -2.0, -6.0, -6.0).getSquareSize());
        assertDoubleEquals(16, new BoundingBox(6.0, 6.0, 2.0, 2.0).getSquareSize());
    }

    @Test
    public void testIntersect() {
        BoundingBox first = new BoundingBox(10.0, 10.0, 0.0, 0.0);
        BoundingBox second = new BoundingBox(15.0, 15.0, 5.0, 5.0);

        BoundingBox intersection = first.intersect(second);

        assertNotNull(intersection);
        assertDoubleEquals(10.0, intersection.northEast().getLongitude());
        assertDoubleEquals(10.0, intersection.northEast().getLatitude());
        assertDoubleEquals(5.0, intersection.southWest().getLongitude());
        assertDoubleEquals(5.0, intersection.southWest().getLatitude());
    }

    @Test
    public void testIntersectWithNullsAndDisjointBoxes() {
        BoundingBox boundingBox = new BoundingBox(10.0, 10.0, 0.0, 0.0);

        assertSame(boundingBox, boundingBox.intersect(null));
        assertNull(new BoundingBox(1.0, 1.0, 0.0, 0.0).intersect(new BoundingBox(3.0, 3.0, 2.0, 2.0)));
    }
}
