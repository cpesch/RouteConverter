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

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PolygonTest {
    // square 0..10 with a hole 4..6
    private final Polygon squareWithHole = Polygon.of(
            List.of(square(0, 0, 10, 10), square(4, 4, 6, 6)), List.of(false, true));
    private final Polygon square = Polygon.of(List.of(square(0, 0, 10, 10)), List.of(false));

    private static List<NavigationPosition> square(double west, double south, double east, double north) {
        return List.of(position(west, south), position(east, south), position(east, north),
                position(west, north), position(west, south));
    }

    private static NavigationPosition position(double longitude, double latitude) {
        return new SimpleNavigationPosition(longitude, latitude);
    }

    private static BoundingBox tile(double west, double south, double east, double north) {
        return new BoundingBox(east, north, west, south);
    }

    @Test
    public void containsPointInsideSquare() {
        assertTrue(square.contains(position(5, 5)));
    }

    @Test
    public void rejectsPointOutside() {
        assertFalse(square.contains(position(11, 5)));
        assertFalse(square.contains(position(5, -1)));
    }

    @Test
    public void rejectsPointInsideAHole() {
        assertFalse(squareWithHole.contains(position(5, 5)));
        assertTrue(squareWithHole.contains(position(2, 2)));
    }

    @Test
    public void intersectsTileFullyInsidePolygon() {
        assertTrue(square.intersects(tile(4, 4, 5, 5)));
    }

    @Test
    public void doesNotIntersectTileFullyInsideAHole() {
        assertFalse(squareWithHole.intersects(tile(4.5, 4.5, 5.5, 5.5)));
    }

    @Test
    public void intersectsTileCrossedByANarrowPolygon() {
        // a thin diagonal sliver through the tile 4..5, whose vertices all are outside of it
        Polygon sliver = Polygon.of(List.of(List.of(position(0, 0), position(10, 10.2), position(10, 10), position(0, -0.2))), List.of(false));
        assertTrue(sliver.intersects(tile(4, 4, 5, 5)));
        assertFalse(sliver.intersects(tile(4, 8, 5, 9)));
    }

    @Test
    public void doesNotIntersectDisjointTile() {
        assertFalse(square.intersects(tile(20, 20, 21, 21)));
    }

    @Test
    public void doesNotIntersectTileInsideTheBoundingBoxButOutsideOfTheShape() {
        Polygon triangle = Polygon.of(List.of(List.of(position(0, 0), position(10, 0), position(0, 10), position(0, 0))), List.of(false));
        assertFalse(triangle.intersects(tile(8, 8, 9, 9)));
        assertTrue(triangle.intersects(tile(1, 1, 2, 2)));
    }
}
