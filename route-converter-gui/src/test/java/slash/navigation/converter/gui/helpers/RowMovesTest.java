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

package slash.navigation.converter.gui.helpers;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.helpers.RowMoves.*;

public class RowMovesTest {
    private static final int ROW_COUNT = 5;

    private static int[] rows(int... rows) {
        return rows;
    }

    @Test
    public void testDeltaDropAbove() {
        assertEquals(-2, delta(rows(3), 1, ROW_COUNT));
        assertEquals(-3, delta(rows(4), 1, ROW_COUNT));
        assertEquals(-2, delta(rows(2, 4), 0, ROW_COUNT));
    }

    @Test
    public void testDeltaDropBelow() {
        assertEquals(2, delta(rows(1), 3, ROW_COUNT));
        assertEquals(2, delta(rows(1, 2), 4, ROW_COUNT));
    }

    @Test
    public void testDeltaDropOntoItselfIsNoMove() {
        assertEquals(0, delta(rows(1, 2), 1, ROW_COUNT));
        assertEquals(0, delta(rows(1, 2), 2, ROW_COUNT));
        assertEquals(0, delta(rows(0), 0, ROW_COUNT));
    }

    @Test
    public void testDeltaDropBelowTheLastRowMovesToTheEnd() {
        assertEquals(1, delta(rows(3), 5, ROW_COUNT));
        assertEquals(1, delta(rows(3), -1, ROW_COUNT));
        assertEquals(1, delta(rows(3), Integer.MAX_VALUE, ROW_COUNT));
    }

    @Test
    public void testDeltaGapsNeverPushARowPastTheEnd() {
        assertEquals(1, delta(rows(1, 3), 4, ROW_COUNT));
    }

    @Test
    public void testDeltaUnsortedRows() {
        int[] rows = {4, 3};
        assertEquals(-3, delta(rows, 0, ROW_COUNT));
        assertArrayEquals(new int[]{4, 3}, rows);
    }

    @Test
    public void testUp() {
        assertArrayEquals(rows(1), up(rows(2), 1));
        assertArrayEquals(rows(0, 1), up(rows(0, 1), 1));
        assertArrayEquals(rows(0, 1), up(rows(0, 2), 1));
        assertArrayEquals(rows(0, 2), up(rows(1, 3), 1));
        assertArrayEquals(rows(0, 2), up(rows(3, 1), 1));
        assertArrayEquals(rows(0, 2), up(rows(2, 4), 2));
        assertArrayEquals(rows(0, 1, 2), up(rows(0, 1, 3), 2));
    }

    @Test
    public void testDown() {
        assertArrayEquals(rows(4), down(rows(4), 1, ROW_COUNT));
        assertArrayEquals(rows(3, 4), down(rows(3, 4), 1, ROW_COUNT));
        assertArrayEquals(rows(2, 4), down(rows(1, 3), 1, ROW_COUNT));
        assertArrayEquals(rows(2, 3, 4), down(rows(1, 3, 4), 2, ROW_COUNT));
    }

    @Test
    public void testTopAndBottom() {
        assertArrayEquals(rows(0, 1), top(rows(2, 4)));
        assertArrayEquals(rows(3, 4), bottom(rows(0, 2), ROW_COUNT));
        assertArrayEquals(rows(), top(rows()));
    }

    @Test
    public void testCanMove() {
        assertFalse(canMoveUp(rows(0)));
        assertFalse(canMoveUp(rows(0, 1)));
        assertFalse(canMoveUp(rows()));
        assertTrue(canMoveUp(rows(0, 2)));
        assertFalse(canMoveDown(rows(4), ROW_COUNT));
        assertFalse(canMoveDown(rows(3, 4), ROW_COUNT));
        assertFalse(canMoveDown(rows(), ROW_COUNT));
        assertTrue(canMoveDown(rows(1, 4), ROW_COUNT));
    }

    @Test
    public void testOrder() {
        assertArrayEquals(rows(0, 2, 1, 3, 4), order(ROW_COUNT, rows(2), rows(1)));
        assertArrayEquals(rows(1, 0, 3, 2, 4), order(ROW_COUNT, rows(1, 3), rows(0, 2)));
        assertArrayEquals(rows(2, 4, 0, 1, 3), order(ROW_COUNT, rows(2, 4), rows(0, 1)));
        assertArrayEquals(rows(1, 3, 4, 0, 2), order(ROW_COUNT, rows(0, 2), rows(3, 4)));
        assertArrayEquals(rows(0, 1, 2, 3, 4), order(ROW_COUNT, rows(), rows()));
    }

    @Test
    public void testInverse() {
        int[][] orders = {rows(0, 2, 1, 3, 4), rows(1, 0, 3, 2, 4), rows(2, 4, 0, 1, 3), rows(1, 3, 4, 0, 2)};
        for (int[] order : orders) {
            int[] inverse = inverse(order);
            for (int i = 0; i < order.length; i++)
                assertEquals(i, inverse[order[i]]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRowBelowZero() {
        up(rows(-1), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRowPastTheEnd() {
        down(rows(5), 1, ROW_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateRow() {
        top(rows(1, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOrderWithDifferentLengths() {
        order(ROW_COUNT, rows(1, 2), rows(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInverseOfNoPermutation() {
        inverse(rows(0, 0, 1));
    }
}
