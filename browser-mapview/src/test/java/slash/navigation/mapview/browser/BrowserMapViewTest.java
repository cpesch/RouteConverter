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

package slash.navigation.mapview.browser;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;

import static slash.common.io.Transfer.ceiling;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class BrowserMapViewTest {
    private static final int MAXIMUM_DIRECTIONS_SEGMENT_LENGTH = 4;

    private List<Integer> createIntervals(int size) {
        List<Integer> result = new ArrayList<>();
        int directionsCount = ceiling(size, MAXIMUM_DIRECTIONS_SEGMENT_LENGTH, false);
        for (int j = 0; j < directionsCount; j++) {
            int start = max(0, j * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH - 1);
            int end = min(size, (j + 1) * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH) - 1;
            for (int i = start + 1; i < end; i++) {
                result.add(i);
            }

            result.add(start);
            result.add(end);
        }
        return result;
    }

    @Test
    public void intervalsBelowSegmentLength() {
        assertEquals(asList(0, 1), createIntervals(2));
        assertEquals(asList(1, 0, 2), createIntervals(3));
        assertEquals(asList(1, 2, 0, 3), createIntervals(4));
    }

    @Test
    public void intervalsAboveSegmentLength() {
        assertEquals(asList(1, 2, 0, 3, 3, 4), createIntervals(5));
        assertEquals(asList(1, 2, 0, 3, 4, 3, 5), createIntervals(6));
        assertEquals(asList(1, 2, 0, 3, 4, 5, 3, 6), createIntervals(7));
        assertEquals(asList(1, 2, 0, 3, 4, 5, 6, 3, 7), createIntervals(8));
    }

    @Test
    public void intervalsAboveDoubleSegmentLength() {
        assertEquals(asList(1, 2, 0, 3, 4, 5, 6, 3, 7, 7, 8), createIntervals(9));
        assertEquals(asList(1, 2, 0, 3, 4, 5, 6, 3, 7, 8, 7, 9), createIntervals(10));
    }
}
