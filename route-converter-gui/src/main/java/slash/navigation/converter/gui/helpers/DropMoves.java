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

import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Computes how far rows dragged within the positions table move when they are dropped
 * onto a row (drop mode {@code ON}).
 *
 * @author Christian Pesch
 */

public class DropMoves {
    private DropMoves() {
    }

    /**
     * Returns the signed number of rows the dragged rows move: negative moves them up,
     * positive moves them down, 0 means no move. All dragged rows move by the same
     * distance, so gaps between them stay.
     *
     * @param rows     the dragged rows, in any order; not modified
     * @param dropRow  the row the rows are dropped onto; outside {@code 0..rowCount} means below the last row
     * @param rowCount the number of rows of the table
     */
    public static int delta(int[] rows, int dropRow, int rowCount) {
        int[] sorted = rows.clone();
        Arrays.sort(sorted);
        int first = sorted[0];
        int last = sorted[sorted.length - 1];

        if (dropRow < 0 || dropRow > rowCount)
            dropRow = rowCount;

        // the first dragged row lands on the drop row
        if (first > dropRow)
            return dropRow - first;

        // the last dragged row of a block lands on the drop row, but no row is pushed past
        // the end and dropping a block onto itself does not move it
        int delta = dropRow - first - sorted.length + 1;
        delta = min(delta, rowCount - 1 - last);
        return max(0, delta);
    }
}
