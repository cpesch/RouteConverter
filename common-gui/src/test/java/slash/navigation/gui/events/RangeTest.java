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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertIntArrayEquals;

public class RangeTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testAsContinuousMonotonicallyIncreasingRanges() {
        assertEquals(asList(asList(0, 1)), Range.asContinuousMonotonicallyIncreasingRanges(new int[]{0, 1}));
        assertEquals(asList(asList(0), asList(2)), Range.asContinuousMonotonicallyIncreasingRanges(new int[]{0, 2}));
        assertEquals(asList(asList(0), asList(2, 3), asList(5, 6, 7)), Range.asContinuousMonotonicallyIncreasingRanges(new int[]{6, 0, 5, 2, 7, 3}));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsContinuousMonotonicallyIncreasingRangesWithLimit() {
        assertEquals(asList(asList(0), asList(1), asList(2)), Range.asContinuousMonotonicallyIncreasingRanges(new int[]{0, 1, 2}, 1));
        assertEquals(asList(asList(0, 1), asList(2, 3), asList(4)), Range.asContinuousMonotonicallyIncreasingRanges(new int[]{0, 1, 2, 3, 4}, 2));
        assertEquals(asList(asList(0, 1), asList(3), asList(5, 6)), Range.asContinuousMonotonicallyIncreasingRanges(new int[]{0, 1, 3, 5, 6}, 2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsContinuousMonotonicallyDecreasingRanges() {
        assertEquals(asList(asList(1, 0)), Range.asContinuousMonotonicallyDecreasingRanges(new int[]{0, 1}));
        assertEquals(asList(asList(2), asList(0)), Range.asContinuousMonotonicallyDecreasingRanges(new int[]{0, 2}));
        assertEquals(asList(asList(7, 6, 5), asList(3, 2), asList(0)), Range.asContinuousMonotonicallyDecreasingRanges(new int[]{3, 6, 0, 7, 5, 2}));
    }

    @Test
    public void testAllButEveryNthAndFirstAndLast() {
        assertIntArrayEquals(new int[]{}, Range.allButEveryNthAndFirstAndLast(0, 1));
        assertIntArrayEquals(new int[]{}, Range.allButEveryNthAndFirstAndLast(0, 100));
        assertIntArrayEquals(new int[]{}, Range.allButEveryNthAndFirstAndLast(1, 1));
        assertIntArrayEquals(new int[]{}, Range.allButEveryNthAndFirstAndLast(1, 100));
        assertIntArrayEquals(new int[]{}, Range.allButEveryNthAndFirstAndLast(2, 1));
        assertIntArrayEquals(new int[]{1}, Range.allButEveryNthAndFirstAndLast(2, 2));
        assertIntArrayEquals(new int[]{1}, Range.allButEveryNthAndFirstAndLast(2, 3));
        assertIntArrayEquals(new int[]{1}, Range.allButEveryNthAndFirstAndLast(2, 100));
        assertIntArrayEquals(new int[]{1}, Range.allButEveryNthAndFirstAndLast(3, 2));
        assertIntArrayEquals(new int[]{1, 2}, Range.allButEveryNthAndFirstAndLast(3, 3));
        assertIntArrayEquals(new int[]{1, 2}, Range.allButEveryNthAndFirstAndLast(3, 4));
        assertIntArrayEquals(new int[]{1, 2}, Range.allButEveryNthAndFirstAndLast(3, 100));
        assertIntArrayEquals(new int[]{1, 3, 5}, Range.allButEveryNthAndFirstAndLast(6, 2));
        assertIntArrayEquals(new int[]{1, 2, 4, 5}, Range.allButEveryNthAndFirstAndLast(6, 3));
        assertIntArrayEquals(new int[]{1, 3, 5, 7, 9}, Range.allButEveryNthAndFirstAndLast(10, 2));
        assertIntArrayEquals(new int[]{1, 2, 4, 5, 7, 8}, Range.allButEveryNthAndFirstAndLast(10, 3));
        assertIntArrayEquals(new int[]{1, 2, 3, 5, 6, 7, 9}, Range.allButEveryNthAndFirstAndLast(10, 4));
    }
}
