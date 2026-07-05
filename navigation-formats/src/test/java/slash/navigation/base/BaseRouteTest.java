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
import slash.common.type.CompactCalendar;
import slash.navigation.common.BoundingBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Tests the shared position-manipulation logic of {@link BaseRoute} through the concrete
 * {@link Wgs84Route} (positions along the zero meridian at latitudes 0..3).
 *
 * @author Christian Pesch
 */
public class BaseRouteTest {

    private static Wgs84Position p(double longitude, double latitude, String description) {
        return new Wgs84Position(longitude, latitude, null, null, null, description);
    }

    private static Wgs84Route route(Wgs84Position... positions) {
        return new Wgs84Route(null, Waypoints, "test", new ArrayList<>(Arrays.asList(positions)));
    }

    private final Wgs84Position p0 = p(0.0, 0.0, "p0");
    private final Wgs84Position p1 = p(0.0, 1.0, "p1");
    private final Wgs84Position p2 = p(0.0, 2.0, "p2");
    private final Wgs84Position p3 = p(0.0, 3.0, "p3");

    @Test
    public void topMovesAPositionUpToTheOffset() {
        Wgs84Route route = route(p0, p1, p2, p3);

        route.top(3, 0);

        assertEquals(p3, route.getPosition(0));
        assertEquals(p0, route.getPosition(1));
        assertEquals(p2, route.getPosition(3));
    }

    @Test
    public void moveSwapsTwoPositions() {
        Wgs84Route route = route(p0, p1, p2, p3);

        route.move(0, 2);

        assertEquals(p2, route.getPosition(0));
        assertEquals(p0, route.getPosition(2));
    }

    @Test
    public void bottomMovesAPositionDownToTheOffset() {
        Wgs84Route route = route(p0, p1, p2, p3);

        route.bottom(0, 0);

        assertEquals(p0, route.getPosition(3));
        assertEquals(p1, route.getPosition(0));
    }

    @Test
    public void removeReturnsThePositionAndShrinksTheRoute() {
        Wgs84Route route = route(p0, p1, p2, p3);

        assertEquals(p1, route.remove(1));
        assertEquals(3, route.getPositionCount());
        assertEquals(p2, route.getPosition(1));
    }

    @Test
    public void removeDuplicatesDropsAdjacentPositionsAtTheSameSpot() {
        Wgs84Route route = route(p(0.0, 0.0, "a"), p(0.0, 0.0, "duplicate"), p(0.0, 1.0, "b"));

        route.removeDuplicates();

        assertEquals(2, route.getPositionCount());
        assertEquals("a", route.getPosition(0).getDescription());
        assertEquals("b", route.getPosition(1).getDescription());
    }

    @Test
    public void getContainedPositionsReturnsIndicesInsideTheBoundingBox() {
        Wgs84Route route = route(p0, p1, p2, p3);
        BoundingBox box = new BoundingBox(0.5, 2.5, -0.5, 0.5); // NE(0.5,2.5) SW(-0.5,0.5)

        assertArrayEquals(new int[]{1, 2}, route.getContainedPositions(box));
    }

    @Test
    public void getPositionsWithinDistanceToPredecessorExcludesEndsAndHonoursTheDistance() {
        Wgs84Route route = route(p0, p1, p2, p3);

        assertArrayEquals(new int[]{1, 2}, route.getPositionsWithinDistanceToPredecessor(300000.0));
        assertArrayEquals(new int[0], route.getPositionsWithinDistanceToPredecessor(1.0));
    }

    @Test
    public void getClosestPositionByCoordinatesRespectsTheThreshold() {
        Wgs84Route route = route(p0, p1, p2, p3);

        assertEquals(2, route.getClosestPosition(0.0, 2.0, 1000.0));
        assertEquals(-1, route.getClosestPosition(0.0, 10.0, 1000.0));
    }

    @Test
    public void getClosestPositionByTimeFindsTheNearestTimestamp() {
        Wgs84Position t1 = new Wgs84Position(0.0, 0.0, null, null, fromMillis(1000), "t1");
        Wgs84Position t2 = new Wgs84Position(0.0, 1.0, null, null, fromMillis(2000), "t2");
        Wgs84Position t3 = new Wgs84Position(0.0, 2.0, null, null, fromMillis(3000), "t3");
        Wgs84Route route = route(t1, t2, t3);

        assertEquals(1, route.getClosestPosition(fromMillis(2100), 1000));
        assertEquals(-1, route.getClosestPosition(fromMillis(9000), 500));
    }

    @Test
    public void successorIndexAndPositionAccessors() {
        Wgs84Route route = route(p0, p1, p2, p3);

        assertEquals(p2, route.getSuccessor(p1));
        assertNull(route.getSuccessor(p3));
        assertEquals(2, route.getIndex(p2));
        assertEquals(p1, route.getPosition(1));
    }

    @Test
    public void getInsignificantPositionsReturnsTheCollinearInteriorPoints() {
        // p0..p3 lie on the same meridian, so the interior points carry no shape information
        assertArrayEquals(new int[]{1, 2}, route(p0, p1, p2, p3).getInsignificantPositions(1000.0));
    }

    @Test
    public void getDistanceDifferenceIsTheStepFromThePredecessor() {
        Wgs84Route route = route(p0, p1, p2, p3);

        assertEquals(0.0, route.getDistanceDifference(0), 0.0);            // no predecessor
        assertEquals(111195.0, route.getDistanceDifference(1), 1000.0);   // ~one degree of latitude
    }

    @Test
    public void getElevationDifferenceIsOtherMinusPredecessorElevation() {
        Wgs84Position e0 = new Wgs84Position(0.0, 0.0, 100.0, null, null, "e0");
        Wgs84Position e1 = new Wgs84Position(0.0, 1.0, 250.0, null, null, "e1");
        Wgs84Route route = route(e0, e1);

        assertEquals(0.0, route.getElevationDifference(0), 0.0);      // no predecessor
        assertEquals(150.0, route.getElevationDifference(1), 0.0);
    }

    @Test
    public void getTimesFromStartAccumulatesTheDeltasByRange() {
        Wgs84Route route = timedRoute();

        assertArrayEquals(new long[]{0, 1000, 3000}, route.getTimesFromStart(0, 2));
    }

    @Test
    public void getTimesFromStartByIndicesReturnsCumulativeTimeAtEachIndex() {
        Wgs84Route route = timedRoute();

        assertArrayEquals(new long[]{1000, 3000}, route.getTimesFromStart(new int[]{1, 2}));
    }

    @Test
    public void getDistancesFromStartByIndicesReturnsCumulativeDistanceAtEachIndex() {
        double[] distances = route(p0, p1, p2, p3).getDistancesFromStart(new int[]{1, 3});

        assertEquals(2, distances.length);
        assertEquals(111195.0, distances[0], 1000.0);   // start -> p1
        assertEquals(333585.0, distances[1], 3000.0);   // start -> p3
    }

    @Test
    public void sortReordersThePositionsByTheComparator() {
        Wgs84Route route = route(p0, p1, p2, p3);

        route.sort(Comparator.comparing(Wgs84Position::getDescription).reversed());

        assertEquals("p3", route.getPosition(0).getDescription());
        assertEquals("p0", route.getPosition(3).getDescription());
    }

    private static Wgs84Route timedRoute() {
        return route(
                new Wgs84Position(0.0, 0.0, null, null, fromMillis(1000), "t0"),
                new Wgs84Position(0.0, 1.0, null, null, fromMillis(2000), "t1"),
                new Wgs84Position(0.0, 2.0, null, null, fromMillis(4000), "t2"));
    }
}
