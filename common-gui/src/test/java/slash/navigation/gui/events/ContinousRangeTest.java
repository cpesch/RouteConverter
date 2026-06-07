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

package slash.navigation.gui.events;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ContinousRange}.
 */
public class ContinousRangeTest {

    /**
     * A simple collecting RangeOperation that records all visited indices and ranges.
     * Never interrupts.
     */
    private static class CollectingOperation implements RangeOperation {
        final List<Integer> indices = new ArrayList<>();
        final List<int[]> ranges = new ArrayList<>();

        @Override
        public void performOnIndex(int index) {
            indices.add(index);
        }

        @Override
        public void performOnRange(int firstIndex, int lastIndex) {
            ranges.add(new int[]{firstIndex, lastIndex});
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }
    }

    // --- performMonotonicallyIncreasing ---

    @Test
    public void testSingleIndexIncreasing() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{5}, op).performMonotonicallyIncreasing();
        assertEquals(List.of(5), op.indices);
        assertEquals(1, op.ranges.size());
        assertEquals(5, op.ranges.get(0)[0]);
        assertEquals(5, op.ranges.get(0)[1]);
    }

    @Test
    public void testContiguousBlockIncreasing() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{2, 3, 4}, op).performMonotonicallyIncreasing();
        assertEquals(List.of(2, 3, 4), op.indices);
        assertEquals(1, op.ranges.size());
        assertEquals(2, op.ranges.get(0)[0]);
        assertEquals(4, op.ranges.get(0)[1]);
    }

    @Test
    public void testTwoSeparateBlocksIncreasing() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{1, 2, 5, 6}, op).performMonotonicallyIncreasing();
        assertEquals(List.of(1, 2, 5, 6), op.indices);
        assertEquals(2, op.ranges.size());
        assertEquals(1, op.ranges.get(0)[0]);
        assertEquals(2, op.ranges.get(0)[1]);
        assertEquals(5, op.ranges.get(1)[0]);
        assertEquals(6, op.ranges.get(1)[1]);
    }

    @Test
    public void testUnsortedIndicesAreSortedBeforeProcessing() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{4, 2, 3}, op).performMonotonicallyIncreasing();
        // after sort: [2, 3, 4] ? one contiguous block
        assertEquals(List.of(2, 3, 4), op.indices);
        assertEquals(1, op.ranges.size());
    }

    // --- performMonotonicallyIncreasing(maxLength) ---

    @Test
    public void testMaxLengthSplitsContiguousBlock() {
        CollectingOperation op = new CollectingOperation();
        // [0,1,2,3,4] with maxLength=3 ? split into [0,1,2] and [3,4]
        new ContinousRange(new int[]{0, 1, 2, 3, 4}, op).performMonotonicallyIncreasing(3);
        assertEquals(List.of(0, 1, 2, 3, 4), op.indices);
        assertEquals(2, op.ranges.size());
        assertEquals(0, op.ranges.get(0)[0]);
        assertEquals(2, op.ranges.get(0)[1]);
        assertEquals(3, op.ranges.get(1)[0]);
        assertEquals(4, op.ranges.get(1)[1]);
    }

    @Test
    public void testMaxLengthLargerThanBlockKeepsOneRange() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{1, 2, 3}, op).performMonotonicallyIncreasing(10);
        assertEquals(1, op.ranges.size());
    }

    // --- performMonotonicallyDecreasing ---

    @Test
    public void testSingleIndexDecreasing() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{7}, op).performMonotonicallyDecreasing();
        assertEquals(List.of(7), op.indices);
        assertEquals(1, op.ranges.size());
        assertEquals(7, op.ranges.get(0)[0]);
        assertEquals(7, op.ranges.get(0)[1]);
    }

    @Test
    public void testContiguousBlockDecreasing() {
        CollectingOperation op = new CollectingOperation();
        // indices [3,4,5] reversed ? [5,4,3], one decreasing block
        new ContinousRange(new int[]{3, 4, 5}, op).performMonotonicallyDecreasing();
        // from=3, to=5 for the range
        assertEquals(1, op.ranges.size());
        int from = op.ranges.get(0)[0];
        int to = op.ranges.get(0)[1];
        assertEquals(3, from);
        assertEquals(5, to);
    }

    @Test
    public void testTwoSeparateBlocksDecreasing() {
        CollectingOperation op = new CollectingOperation();
        new ContinousRange(new int[]{1, 2, 5, 6}, op).performMonotonicallyDecreasing();
        // after revert: [6,5,2,1] ? blocks [6,5] and [2,1]
        assertEquals(2, op.ranges.size());
    }

    // --- Range.allButEveryNthAndFirstAndLast ---

    @Test
    public void testAllButEveryNthNth1ReturnsEmpty() {
        // With nth=1 every interval has zero interior points: inner loop j < (i+1-1)=i is never true
        int[] result = Range.allButEveryNthAndFirstAndLast(5, 1);
        assertEquals(0, result.length);
    }

    @Test
    public void testAllButEveryNthNth2() {
        // nth=2, maximum=5 ? intervals [1,2),[3,4) ? interior points 1,3
        int[] result = Range.allButEveryNthAndFirstAndLast(5, 2);
        assertEquals(2, result.length);
        assertEquals(1, result[0]);
        assertEquals(3, result[1]);
    }

    @Test
    public void testAllButEveryNthNth3() {
        // nth=3, maximum=7 ? intervals [1,3),[4,6) ? interior points 1,2,4,5
        int[] result = Range.allButEveryNthAndFirstAndLast(7, 3);
        assertEquals(4, result.length);
        for (int v : result) {
            assertTrue(v >= 1 && v < 7);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllButEveryNthThrowsForZeroNth() {
        Range.allButEveryNthAndFirstAndLast(5, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllButEveryNthThrowsForNegativeNth() {
        Range.allButEveryNthAndFirstAndLast(5, -1);
    }
}

