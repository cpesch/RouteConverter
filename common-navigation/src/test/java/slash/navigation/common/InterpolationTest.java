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

import static java.lang.Math.floor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class InterpolationTest {
    // a meridian leg near the equator: ~1105 m long (0.01 deg latitude).
    private static final double LON1 = 0.0, LAT1 = 0.0, LON2 = 0.0, LAT2 = 0.01;

    private static double legDistance() {
        return Bearing.calculateBearing(LON1, LAT1, LON2, LAT2).getDistance();
    }

    @Test
    public void testEvenSplitInteriorPointsAtFractions() {
        double interval = 250;
        double distance = legDistance();
        int n = (int) floor(distance / interval);   // ~4 for a ~1105 m leg
        assertTrue("leg should span at least two intervals", n >= 2);

        List<double[]> points = Interpolation.interpolate(LON1, LAT1, LON2, LAT2, interval);

        // n-1 interior points, at fractions i/n, endpoints excluded
        assertEquals(n - 1, points.size());
        for (int i = 1; i < n; i++) {
            double t = (double) i / n;
            double[] p = points.get(i - 1);
            assertDoubleEquals(LON1 + t * (LON2 - LON1), p[0]);
            assertDoubleEquals(LAT1 + t * (LAT2 - LAT1), p[1]);
        }
    }

    @Test
    public void testEndpointsExcluded() {
        List<double[]> points = Interpolation.interpolate(LON1, LAT1, LON2, LAT2, 250);
        assertTrue(!points.isEmpty());
        double[] first = points.get(0), last = points.get(points.size() - 1);
        assertTrue("first interior point must not equal a", first[1] != LAT1);
        assertTrue("last interior point must not equal b", last[1] != LAT2);
    }

    @Test
    public void testMonotonicOrderingFromAToB() {
        List<double[]> points = Interpolation.interpolate(LON1, LAT1, LON2, LAT2, 250);
        double previous = LAT1;
        for (double[] p : points) {
            assertTrue("latitude must increase strictly a -> b", p[1] > previous);
            assertTrue("latitude must stay below b", p[1] < LAT2);
            previous = p[1];
        }
    }

    @Test
    public void testDistanceNotGreaterThanIntervalInsertsNothing() {
        double interval = legDistance() * 2.0;   // leg shorter than one interval
        assertTrue(Interpolation.interpolate(LON1, LAT1, LON2, LAT2, interval).isEmpty());
    }

    @Test
    public void testExactlyOneIntervalInsertsNothing() {
        // distance/interval in [1,2) -> n < 2 -> no interior point
        double interval = legDistance();
        assertTrue(Interpolation.interpolate(LON1, LAT1, LON2, LAT2, interval).isEmpty());
    }

    @Test
    public void testNonPositiveIntervalInsertsNothing() {
        assertTrue(Interpolation.interpolate(LON1, LAT1, LON2, LAT2, 0).isEmpty());
        assertTrue(Interpolation.interpolate(LON1, LAT1, LON2, LAT2, -250).isEmpty());
    }
}
