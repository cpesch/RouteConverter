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
import static slash.navigation.converter.gui.helpers.DropMoves.delta;

public class DropMovesTest {
    private static final int ROW_COUNT = 5;

    @Test
    public void testDropAbove() {
        assertEquals(-2, delta(new int[]{3}, 1, ROW_COUNT));
        assertEquals(-3, delta(new int[]{4}, 1, ROW_COUNT));
        assertEquals(-2, delta(new int[]{2, 4}, 0, ROW_COUNT));
    }

    @Test
    public void testDropBelow() {
        assertEquals(2, delta(new int[]{1}, 3, ROW_COUNT));
        assertEquals(2, delta(new int[]{1, 2}, 4, ROW_COUNT));
    }

    @Test
    public void testDropOntoItselfIsNoMove() {
        assertEquals(0, delta(new int[]{1, 2}, 1, ROW_COUNT));
        assertEquals(0, delta(new int[]{1, 2}, 2, ROW_COUNT));
        assertEquals(0, delta(new int[]{0}, 0, ROW_COUNT));
    }

    @Test
    public void testDropBelowTheLastRowMovesToTheEnd() {
        assertEquals(1, delta(new int[]{3}, 5, ROW_COUNT));
        assertEquals(1, delta(new int[]{3}, -1, ROW_COUNT));
        assertEquals(1, delta(new int[]{3}, Integer.MAX_VALUE, ROW_COUNT));
    }

    @Test
    public void testGapsNeverPushARowPastTheEnd() {
        assertEquals(1, delta(new int[]{1, 3}, 4, ROW_COUNT));
    }

    @Test
    public void testUnsortedRows() {
        int[] rows = {4, 3};
        assertEquals(-3, delta(rows, 0, ROW_COUNT));
        assertArrayEquals(new int[]{4, 3}, rows);
    }
}
