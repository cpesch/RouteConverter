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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.ElevationSums.ascend;
import static slash.navigation.base.ElevationSums.descend;

public class ElevationSumsTest {
    private static final List<Double> JITTER = Arrays.asList(100.0, 101.0, 100.0, 101.0, 100.0);

    @Test
    public void thresholdZeroEqualsRawSum() {
        assertEquals(2.0, ascend(JITTER, 0.0), 0.0);
        assertEquals(2.0, descend(JITTER, 0.0), 0.0);
    }

    @Test
    public void jitterBelowThresholdIsIgnored() {
        assertEquals(0.0, ascend(JITTER, 5.0), 0.0);
        assertEquals(0.0, descend(JITTER, 5.0), 0.0);
    }

    @Test
    public void steadyClimbCountsAgainstLastCountedElevation() {
        List<Double> elevations = Arrays.asList(100.0, 103.0, 106.0, 109.0, 112.0);
        assertEquals(12.0, ascend(elevations, 5.0), 0.0);
        assertEquals(0.0, descend(elevations, 5.0), 0.0);
    }

    @Test
    public void smallDipIsIgnored() {
        List<Double> elevations = Arrays.asList(100.0, 110.0, 108.0, 115.0);
        assertEquals(15.0, ascend(elevations, 5.0), 0.0);
        assertEquals(0.0, descend(elevations, 5.0), 0.0);
    }

    @Test
    public void thresholdIsInclusive() {
        assertEquals(5.0, ascend(Arrays.asList(100.0, 105.0), 5.0), 0.0);
        assertEquals(5.0, descend(Arrays.asList(105.0, 100.0), 5.0), 0.0);
    }

    @Test
    public void trailingRemainderIsDropped() {
        assertEquals(10.0, ascend(Arrays.asList(100.0, 110.0, 113.0), 5.0), 0.0);
    }

    @Test
    public void nullElevationsAreSkipped() {
        assertEquals(10.0, ascend(Arrays.asList(100.0, null, 110.0), 5.0), 0.0);
    }

    @Test
    public void degenerateListsSumToZero() {
        List<Double> empty = emptyList();
        assertEquals(0.0, ascend(empty, 5.0), 0.0);
        assertEquals(0.0, descend(empty, 5.0), 0.0);
        assertEquals(0.0, ascend(singletonList(100.0), 5.0), 0.0);
        assertEquals(0.0, descend(singletonList(100.0), 5.0), 0.0);
        assertEquals(0.0, ascend(Arrays.asList(null, null), 5.0), 0.0);
        assertEquals(0.0, descend(Arrays.asList(null, null), 5.0), 0.0);
    }

    @Test
    public void negativeThresholdBehavesLikeZero() {
        assertEquals(2.0, ascend(JITTER, -3.0), 0.0);
        assertEquals(2.0, descend(JITTER, -3.0), 0.0);
    }

    @Test
    public void noisyClimbIsCloseToTheRealGain() {
        List<Double> elevations = new ArrayList<>();
        for (int i = 0; i < 1000; i++)
            elevations.add(i * 500.0 / 999 + (i % 2 == 0 ? 2.0 : -2.0));

        assertEquals(500.0, ascend(elevations, 5.0), 10.0);
        assertTrue(descend(elevations, 5.0) < 10.0);
        assertTrue(ascend(elevations, 0.0) > 2000.0);
    }
}
