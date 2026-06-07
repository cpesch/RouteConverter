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

package slash.navigation.tour;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for {@link PositionInListComparator}.
 *
 * @author Christian Pesch
 */

public class PositionInListComparatorTest {

    private TourPosition positionWithIndex(int index) {
        Map<String, String> values = new HashMap<>();
        values.put(TourFormat.POSITION_IN_LIST, String.valueOf(index));
        return new TourPosition(0L, 0L, null, "city", null, null, null, false, values);
    }

    private TourPosition positionWithoutIndex() {
        Map<String, String> values = new HashMap<>();
        return new TourPosition(0L, 0L, null, "city", null, null, null, false, values);
    }

    @Test
    public void testEqualIndex() {
        PositionInListComparator comparator = new PositionInListComparator();
        TourPosition p1 = positionWithIndex(5);
        TourPosition p2 = positionWithIndex(5);
        assertEquals(0, comparator.compare(p1, p2));
    }

    @Test
    public void testLowerIndexComesFirst() {
        PositionInListComparator comparator = new PositionInListComparator();
        TourPosition p1 = positionWithIndex(3);
        TourPosition p2 = positionWithIndex(7);
        assertTrue(comparator.compare(p1, p2) < 0);
    }

    @Test
    public void testHigherIndexComesLast() {
        PositionInListComparator comparator = new PositionInListComparator();
        TourPosition p1 = positionWithIndex(10);
        TourPosition p2 = positionWithIndex(2);
        assertTrue(comparator.compare(p1, p2) > 0);
    }

    @Test
    public void testZeroAndPositive() {
        PositionInListComparator comparator = new PositionInListComparator();
        TourPosition p1 = positionWithIndex(0);
        TourPosition p2 = positionWithIndex(1);
        assertTrue(comparator.compare(p1, p2) < 0);
    }

    @Test
    public void testPositionsWithoutIndexUseHashCode() {
        PositionInListComparator comparator = new PositionInListComparator();
        TourPosition p1 = positionWithoutIndex();
        TourPosition p2 = positionWithoutIndex();
        // compare result is hashCode difference ? just verify it doesn't throw
        int result = comparator.compare(p1, p2);
        // same object compared to itself is 0
        assertEquals(0, comparator.compare(p1, p1));
    }
}

