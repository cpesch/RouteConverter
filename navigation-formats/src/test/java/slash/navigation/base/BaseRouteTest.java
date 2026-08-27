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
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;
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
    public void getInsignificantPositionsReturnsTheCollinearInteriorPoints() throws InterruptedException {
        // p0..p3 lie on the same meridian, so the interior points carry no shape information
        assertArrayEquals(new int[]{1, 2}, route(p0, p1, p2, p3).getInsignificantPositions(1000.0));
    }

    @Test
    public void getPositionsWithSpeedZeroFindsExactMatches() {
        Wgs84Position p0 = new Wgs84Position(0.0, 0.0, null, null, null, "p0"); // speed null
        Wgs84Position p1 = new Wgs84Position(0.0, 1.0, null, 1.0, null, "p1"); // speed 1.0
        Wgs84Position p2 = new Wgs84Position(0.0, 2.0, null, 0.0, null, "p2"); // speed 0.0 (pause)
        Wgs84Position p3 = new Wgs84Position(0.0, 3.0, null, 0.0, null, "p3"); // speed 0.0 (pause)
        Wgs84Position p4 = new Wgs84Position(0.0, 4.0, null, 5.0, null, "p4"); // speed 5.0
        Wgs84Route route = route(p0, p1, p2, p3, p4);

        assertArrayEquals(new int[]{2, 3}, route.getPositionsWithSpeedZero());
    }

    @Test
    public void getPositionsWithSpeedZeroReturnsEmptyArrayWhenNoneMatch() {
        Wgs84Position p0 = new Wgs84Position(0.0, 0.0, null, null, null, "p0"); // speed null
        Wgs84Position p1 = new Wgs84Position(0.0, 1.0, null, 1.0, null, "p1"); // speed 1.0
        Wgs84Position p2 = new Wgs84Position(0.0, 2.0, null, 5.0, null, "p2"); // speed 5.0
        Wgs84Route route = route(p0, p1, p2);

        assertArrayEquals(new int[0], route.getPositionsWithSpeedZero());
    }

    @Test
    public void shiftTimesAfterPausesShiftsOnlyPositionsAfterTheBlock() {
        Wgs84Position p0 = new Wgs84Position(0.0, 0.0, null, null, fromMillis(0), "p0");
        Wgs84Position p1 = new Wgs84Position(0.0, 1.0, null, 0.0, fromMillis(10000), "p1"); // pause
        Wgs84Position p2 = new Wgs84Position(0.0, 2.0, null, 0.0, fromMillis(40000), "p2"); // pause
        Wgs84Position p3 = new Wgs84Position(0.0, 3.0, null, null, fromMillis(50000), "p3");
        Wgs84Route route = route(p0, p1, p2, p3);

        // Pause block = indices {1,2} (times 10s..40s, duration 30s)
        route.shiftTimesAfterPauses(new int[]{1, 2});

        // Position 0 unchanged, position 3 shifted back by 30s (50s - 30s = 20s)
        assertEquals(0, route.getPosition(0).getTime().getTimeInMillis());
        assertEquals(10000, route.getPosition(1).getTime().getTimeInMillis()); // unchanged
        assertEquals(40000, route.getPosition(2).getTime().getTimeInMillis()); // unchanged
        assertEquals(20000, route.getPosition(3).getTime().getTimeInMillis()); // 50s - 30s = 20s
    }

    @Test
    public void shiftTimesAfterPausesHandlesMultipleBlocksCumulatively() {
        Wgs84Position p0 = new Wgs84Position(0.0, 0.0, null, null, fromMillis(0), "p0");
        Wgs84Position p1 = new Wgs84Position(0.0, 1.0, null, 0.0, fromMillis(10000), "p1"); // pause block 1
        Wgs84Position p2 = new Wgs84Position(0.0, 2.0, null, 0.0, fromMillis(20000), "p2"); // pause block 1
        Wgs84Position p3 = new Wgs84Position(0.0, 3.0, null, null, fromMillis(30000), "p3");
        Wgs84Position p4 = new Wgs84Position(0.0, 4.0, null, 0.0, fromMillis(50000), "p4"); // pause block 2
        Wgs84Position p5 = new Wgs84Position(0.0, 5.0, null, null, fromMillis(60000), "p5");
        Wgs84Route route = route(p0, p1, p2, p3, p4, p5);

        // Pause block 1 = indices {1,2} (times 10s..20s, duration 10s)
        // Pause block 2 = indices {4} (times 50s, duration 0s for single point)
        // Note: single-point blocks have first=last, so duration = 0
        route.shiftTimesAfterPauses(new int[]{1, 2, 4});

        // Position 0 unchanged
        assertEquals(0, route.getPosition(0).getTime().getTimeInMillis());
        // Positions 1,2 unchanged (about to be deleted)
        assertEquals(10000, route.getPosition(1).getTime().getTimeInMillis());
        assertEquals(20000, route.getPosition(2).getTime().getTimeInMillis());
        // Position 3 shifted by block 1's duration (30s - 10s = 20s)
        assertEquals(20000, route.getPosition(3).getTime().getTimeInMillis());
        // Position 4 unchanged (about to be deleted)
        assertEquals(50000, route.getPosition(4).getTime().getTimeInMillis());
        // Position 5 shifted by block 1's duration (60s - 10s = 50s)
        assertEquals(50000, route.getPosition(5).getTime().getTimeInMillis());
    }

    @Test
    public void shiftTimesAfterPausesSkipsBlockWithMissingTime() {
        Wgs84Position p0 = new Wgs84Position(0.0, 0.0, null, null, fromMillis(0), "p0");
        Wgs84Position p1 = new Wgs84Position(0.0, 1.0, null, 0.0, null, "p1"); // pause, no time
        Wgs84Position p2 = new Wgs84Position(0.0, 2.0, null, 0.0, fromMillis(20000), "p2"); // pause
        Wgs84Position p3 = new Wgs84Position(0.0, 3.0, null, null, fromMillis(30000), "p3");
        Wgs84Route route = route(p0, p1, p2, p3);

        // Pause block = indices {1,2}, but position 1 has no time
        route.shiftTimesAfterPauses(new int[]{1, 2});

        // No times should have changed
        assertEquals(0, route.getPosition(0).getTime().getTimeInMillis());
        assertNull(route.getPosition(1).getTime());
        assertEquals(20000, route.getPosition(2).getTime().getTimeInMillis());
        assertEquals(30000, route.getPosition(3).getTime().getTimeInMillis());
    }

    @Test
    public void shiftTimesAfterPausesIsNoOpForEmptyIndices() {
        Wgs84Position p0 = new Wgs84Position(0.0, 0.0, null, null, fromMillis(0), "p0");
        Wgs84Position p1 = new Wgs84Position(0.0, 1.0, null, null, fromMillis(10000), "p1");
        Wgs84Route route = route(p0, p1);

        route.shiftTimesAfterPauses(new int[0]);

        // No times should have changed
        assertEquals(0, route.getPosition(0).getTime().getTimeInMillis());
        assertEquals(10000, route.getPosition(1).getTime().getTimeInMillis());
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
