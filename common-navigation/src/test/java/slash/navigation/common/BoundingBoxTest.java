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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoundingBoxTest {

    // A box from (10W, 40N south-west) to (20E, 50N north-east)
    private final BoundingBox box = new BoundingBox(20.0, 50.0, 10.0, 40.0);

    // --- contains(NavigationPosition) ---

    @Test
    public void testContainsPositionInsideBox() {
        SimpleNavigationPosition inside = new SimpleNavigationPosition(15.0, 45.0);
        assertTrue(box.contains(inside));
    }

    @Test
    public void testContainsPositionOnSouthWestCorner() {
        SimpleNavigationPosition swCorner = new SimpleNavigationPosition(10.0, 40.0);
        assertTrue(box.contains(swCorner));
    }

    @Test
    public void testContainsPositionOnNorthEastCorner() {
        SimpleNavigationPosition neCorner = new SimpleNavigationPosition(20.0, 50.0);
        assertTrue(box.contains(neCorner));
    }

    @Test
    public void testDoesNotContainPositionWestOfBox() {
        SimpleNavigationPosition west = new SimpleNavigationPosition(9.9, 45.0);
        assertFalse(box.contains(west));
    }

    @Test
    public void testDoesNotContainPositionEastOfBox() {
        SimpleNavigationPosition east = new SimpleNavigationPosition(20.1, 45.0);
        assertFalse(box.contains(east));
    }

    @Test
    public void testDoesNotContainPositionSouthOfBox() {
        SimpleNavigationPosition south = new SimpleNavigationPosition(15.0, 39.9);
        assertFalse(box.contains(south));
    }

    @Test
    public void testDoesNotContainPositionNorthOfBox() {
        SimpleNavigationPosition north = new SimpleNavigationPosition(15.0, 50.1);
        assertFalse(box.contains(north));
    }

    // --- contains(BoundingBox) ---

    @Test
    public void testContainsSmallerBoxInsideItself() {
        BoundingBox smaller = new BoundingBox(18.0, 48.0, 12.0, 42.0);
        assertTrue(box.contains(smaller));
    }

    @Test
    public void testDoesNotContainLargerBox() {
        BoundingBox larger = new BoundingBox(25.0, 55.0, 5.0, 35.0);
        assertFalse(box.contains(larger));
    }

    @Test
    public void testDoesNotContainPartiallyOverlappingBox() {
        BoundingBox partial = new BoundingBox(25.0, 48.0, 12.0, 42.0);
        assertFalse(box.contains(partial));
    }

    // --- intersect ---

    @Test
    public void testIntersectWithOverlappingBox() {
        BoundingBox other = new BoundingBox(25.0, 55.0, 15.0, 45.0);
        BoundingBox intersection = box.intersect(other);
        assertNotNull(intersection);
        assertEquals(20.0, intersection.northEast().getLongitude(), 0.001);
        assertEquals(50.0, intersection.northEast().getLatitude(), 0.001);
        assertEquals(15.0, intersection.southWest().getLongitude(), 0.001);
        assertEquals(45.0, intersection.southWest().getLatitude(), 0.001);
    }

    @Test
    public void testIntersectWithNonOverlappingBoxReturnsNull() {
        BoundingBox disjoint = new BoundingBox(40.0, 70.0, 30.0, 60.0);
        assertNull(box.intersect(disjoint));
    }

    @Test
    public void testIntersectWithNullReturnsSelf() {
        BoundingBox result = box.intersect(null);
        assertEquals(box, result);
    }

    // --- getCenter ---

    @Test
    public void testGetCenter() {
        NavigationPosition center = box.getCenter();
        assertEquals(15.0, center.getLongitude(), 0.001);
        assertEquals(45.0, center.getLatitude(), 0.001);
    }

    @Test
    public void testGetCenterSymmetricBox() {
        BoundingBox symmetric = new BoundingBox(10.0, 10.0, -10.0, -10.0);
        NavigationPosition center = symmetric.getCenter();
        assertEquals(0.0, center.getLongitude(), 0.001);
        assertEquals(0.0, center.getLatitude(), 0.001);
    }

    // --- getSquareSize ---

    @Test
    public void testGetSquareSizeIsPositive() {
        assertTrue(box.getSquareSize() > 0.0);
    }

    @Test
    public void testGetSquareSizeKnownValue() {
        // (20-10) * (50-40) = 100
        assertEquals(100.0, box.getSquareSize(), 0.001);
    }

    // --- asBoundingBox ---

    @Test
    public void testAsBoundingBoxFromPositionList() {
        List<SimpleNavigationPosition> positions = Arrays.asList(
                new SimpleNavigationPosition(10.0, 40.0),
                new SimpleNavigationPosition(20.0, 50.0),
                new SimpleNavigationPosition(15.0, 45.0)
        );
        BoundingBox bb = BoundingBox.asBoundingBox(positions);
        assertNotNull(bb);
        assertEquals(20.0, bb.northEast().getLongitude(), 0.001);
        assertEquals(50.0, bb.northEast().getLatitude(), 0.001);
        assertEquals(10.0, bb.southWest().getLongitude(), 0.001);
        assertEquals(40.0, bb.southWest().getLatitude(), 0.001);
    }

    // --- getSouthEast / getNorthWest ---

    @Test
    public void testGetSouthEast() {
        NavigationPosition se = box.getSouthEast();
        assertEquals(20.0, se.getLongitude(), 0.001);
        assertEquals(40.0, se.getLatitude(), 0.001);
    }

    @Test
    public void testGetNorthWest() {
        NavigationPosition nw = box.getNorthWest();
        assertEquals(10.0, nw.getLongitude(), 0.001);
        assertEquals(50.0, nw.getLatitude(), 0.001);
    }

    // --- toString ---

    @Test
    public void testToStringContainsClassName() {
        String s = box.toString();
        assertTrue(s.contains("BoundingBox"));
    }
}

