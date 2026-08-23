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
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;

public class RouteCalculationsTest {

    private static Wgs84Position pos(double lon, double lat) {
        return new Wgs84Position(lon, lat, null, null, null, null);
    }

    private static Wgs84Position posWithTime(double lon, double lat, long millis) {
        return new Wgs84Position(lon, lat, null, null, fromMillis(millis), null);
    }

    /**
     * A GPS-like random walk of {@code n} positions, deterministic for a given {@code seed}.
     */
    private static List<Wgs84Position> randomWalk(int n, long seed) {
        Random random = new Random(seed);
        List<Wgs84Position> positions = new ArrayList<>(n);
        double longitude = 10.0, latitude = 50.0;
        for (int i = 0; i < n; i++) {
            positions.add(pos(longitude, latitude));
            longitude += (random.nextDouble() - 0.5) * 0.0002;
            latitude += (random.nextDouble() - 0.5) * 0.0002;
        }
        return positions;
    }

    /**
     * A track of {@code n} positions whose deviation from the start-to-end chord decays
     * monotonically along the track, forcing Douglas-Peucker to split off a single point at
     * (or near) the low end of every remaining segment - the classic unbalanced/worst-case shape.
     */
    private static List<Wgs84Position> unbalancedTrack(int n) {
        List<Wgs84Position> positions = new ArrayList<>(n);
        double longitudeStep = 0.0005;
        double metersPerDegreeLatitude = 111_320.0;
        double amplitudeMeters = 2.0 * 30.0 * (n - 1);
        for (int i = 0; i < n; i++) {
            double longitude = i * longitudeStep;
            double deviationMeters = (i == 0 || i == n - 1) ? 0.0 : amplitudeMeters / i;
            double latitude = deviationMeters / metersPerDegreeLatitude;
            positions.add(pos(longitude, latitude));
        }
        return positions;
    }

    // --- getSignificantPositions ---

    @Test
    public void testEmptyListReturnsEmptyArray() throws InterruptedException {
        int[] result = RouteCalculations.getSignificantPositions(Collections.emptyList(), 10.0);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testSinglePositionReturnsSingleIndex() throws InterruptedException {
        List<Wgs84Position> positions = Collections.singletonList(pos(10.0, 50.0));
        int[] result = RouteCalculations.getSignificantPositions(positions, 10.0);
        assertArrayEquals(new int[]{0}, result);
    }

    @Test
    public void testTwoPositionsReturnsBothEndpoints() throws InterruptedException {
        List<Wgs84Position> positions = Arrays.asList(pos(10.0, 50.0), pos(11.0, 51.0));
        int[] result = RouteCalculations.getSignificantPositions(positions, 10.0);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void testThreeCollinearPositionsHighThresholdReturnsTwoEndpoints() throws InterruptedException {
        // Three points on roughly the same line ? the middle one is within a large threshold
        List<Wgs84Position> positions = Arrays.asList(
                pos(10.0, 50.0),
                pos(10.5, 50.5),  // midpoint on the line
                pos(11.0, 51.0)
        );
        // Very high threshold: middle point is not significant
        int[] result = RouteCalculations.getSignificantPositions(positions, 100_000.0);
        assertArrayEquals(new int[]{0, 2}, result);
    }

    @Test
    public void testThreeSpreadPositionsLowThresholdKeepsMiddle() throws InterruptedException {
        // Middle point deviates far from the straight line at low threshold
        List<Wgs84Position> positions = Arrays.asList(
                pos(10.0, 50.0),
                pos(10.5, 51.0),  // far off the line between 0 and 2
                pos(11.0, 50.0)
        );
        // Very low threshold: all three points are significant
        int[] result = RouteCalculations.getSignificantPositions(positions, 0.001);
        // must contain indices 0, 1, and 2
        List<Integer> indices = new ArrayList<>();
        for (int i : result) indices.add(i);
        assertTrue(indices.contains(0));
        assertTrue(indices.contains(1));
        assertTrue(indices.contains(2));
    }

    @Test
    public void testLargeTrackCompletesQuickly() throws InterruptedException {
        List<Wgs84Position> positions = randomWalk(113934, 42);
        long startedAt = System.nanoTime();
        int[] result = RouteCalculations.getSignificantPositions(positions, 30.0);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        assertTrue("Expected under 5000 ms, took " + elapsedMs + " ms", elapsedMs < 5000);
        assertTrue(result.length > 0);
    }

    @Test
    public void testUnbalancedTrackCompletesQuickly() throws InterruptedException {
        List<Wgs84Position> positions = unbalancedTrack(113934);
        long startedAt = System.nanoTime();
        int[] result = RouteCalculations.getSignificantPositions(positions, 30.0);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        assertTrue("Expected under 60000 ms, took " + elapsedMs + " ms", elapsedMs < 60000);
        assertTrue(result.length > 0);
    }

    @Test(expected = InterruptedException.class)
    public void testInterruptedComputationThrows() throws InterruptedException {
        List<Wgs84Position> positions = unbalancedTrack(50000);
        Thread.currentThread().interrupt();
        try {
            RouteCalculations.getSignificantPositions(positions, 30.0);
        } finally {
            Thread.interrupted(); // clear the flag so it doesn't leak into later tests
        }
    }

    @Test
    public void testCollinearChordEndpointsDoNotDivideByZero() throws InterruptedException {
        // first and last position share the same coordinates, so the chord is a single point
        List<Wgs84Position> positions = Arrays.asList(
                pos(10.0, 50.0),
                pos(10.5, 50.5),
                pos(11.0, 51.0),
                pos(10.0, 50.0)
        );
        int[] result = RouteCalculations.getSignificantPositions(positions, 10.0);
        assertNotNull(result);
        List<Integer> indices = new ArrayList<>();
        for (int i : result) indices.add(i);
        assertTrue(indices.contains(0));
        assertTrue(indices.contains(positions.size() - 1));
    }

    @Test
    public void testAntimeridianCrossingGivesSameResultAsUnshiftedTrack() throws InterruptedException {
        // Douglas-Peucker only depends on relative geometry, so shifting every longitude by
        // the same amount - even across the +/-180 degree antimeridian - must not change which
        // positions are kept; a projection that doesn't wrap longitude deltas would break this.
        List<Wgs84Position> baseline = unbalancedTrack(300);
        double shiftDegrees = 179.95; // baseline spans [0, 0.1495] degrees, so this wraps roughly the back half
        List<Wgs84Position> shifted = new ArrayList<>(baseline.size());
        for (Wgs84Position position : baseline) {
            double shiftedLongitude = position.getLongitude() + shiftDegrees;
            if (shiftedLongitude > 180.0)
                shiftedLongitude -= 360.0;
            shifted.add(pos(shiftedLongitude, position.getLatitude()));
        }

        int[] baselineResult = RouteCalculations.getSignificantPositions(baseline, 30.0);
        int[] shiftedResult = RouteCalculations.getSignificantPositions(shifted, 30.0);

        assertArrayEquals("Simplification result should be invariant under a longitude shift, " +
                        "including across the antimeridian",
                baselineResult, shiftedResult);
    }

    // --- asWgs84Position ---

    @Test
    public void testAsWgs84PositionTwoArgs() {
        Wgs84Position p = RouteCalculations.asWgs84Position(10.5, 48.3);
        assertEquals(10.5, p.getLongitude(), 0.0001);
        assertEquals(48.3, p.getLatitude(), 0.0001);
        assertNull(p.getDescription());
        assertNull(p.getElevation());
        assertNull(p.getTime());
    }

    @Test
    public void testAsWgs84PositionThreeArgs() {
        Wgs84Position p = RouteCalculations.asWgs84Position(10.5, 48.3, "Munich");
        assertEquals(10.5, p.getLongitude(), 0.0001);
        assertEquals(48.3, p.getLatitude(), 0.0001);
        assertEquals("Munich", p.getDescription());
    }

    // --- interpolateTime ---

    @Test
    public void testInterpolateTimeNullPredecessorTimeReturnsNull() {
        Wgs84Position pos = pos(10.5, 50.5);
        Wgs84Position pred = pos(10.0, 50.0);  // no time
        Wgs84Position succ = posWithTime(11.0, 51.0, 2000L);
        assertNull(RouteCalculations.interpolateTime(pos, pred, succ));
    }

    @Test
    public void testInterpolateTimeNullSuccessorTimeReturnsNull() {
        Wgs84Position pos = pos(10.5, 50.5);
        Wgs84Position pred = posWithTime(10.0, 50.0, 0L);
        Wgs84Position succ = pos(11.0, 51.0);  // no time
        assertNull(RouteCalculations.interpolateTime(pos, pred, succ));
    }

    @Test
    public void testInterpolateTimeAtMidpointReturnsHalfwayTime() {
        // pred at t=0, succ at t=2000; midpoint should get t?1000
        Wgs84Position pred = posWithTime(10.0, 50.0, 0L);
        Wgs84Position succ = posWithTime(12.0, 50.0, 2000L);
        Wgs84Position mid = pos(11.0, 50.0);
        CompactCalendar interpolated = RouteCalculations.interpolateTime(mid, pred, succ);
        assertNotNull(interpolated);
        // midpoint in lon ? ? 1000 ms
        long ms = interpolated.getTimeInMillis();
        assertTrue("Expected ~1000ms, got " + ms, ms > 500 && ms < 1500);
    }

    // --- extrapolateTime ---

    @Test
    public void testExtrapolateTimeNullBeforePredecessorTimeReturnsNull() {
        Wgs84Position pos = pos(11.0, 50.0);
        Wgs84Position pred = posWithTime(10.5, 50.0, 1000L);
        Wgs84Position before = pos(10.0, 50.0);  // no time
        assertNull(RouteCalculations.extrapolateTime(pos, pred, before));
    }

    @Test
    public void testExtrapolateTimeNullPredecessorTimeReturnsNull() {
        Wgs84Position pos = pos(11.0, 50.0);
        Wgs84Position pred = pos(10.5, 50.0);  // no time
        Wgs84Position before = posWithTime(10.0, 50.0, 0L);
        assertNull(RouteCalculations.extrapolateTime(pos, pred, before));
    }

    @Test
    public void testExtrapolateTimeProjectsBeyondLastPoint() {
        // before at t=0, pred at t=1000 (1° lon apart); next point 1° further should get t?2000
        Wgs84Position before = posWithTime(10.0, 50.0, 0L);
        Wgs84Position pred = posWithTime(11.0, 50.0, 1000L);
        Wgs84Position next = pos(12.0, 50.0);
        CompactCalendar extrapolated = RouteCalculations.extrapolateTime(next, pred, before);
        assertNotNull(extrapolated);
        long ms = extrapolated.getTimeInMillis();
        assertTrue("Expected ~2000ms, got " + ms, ms > 1500 && ms < 2500);
    }
}

