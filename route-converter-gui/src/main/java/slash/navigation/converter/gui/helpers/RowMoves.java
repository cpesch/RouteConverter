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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Computes where marked rows of a table or list end up when they are moved, for the
 * positions table and the position lists. Pure arithmetic, the callers apply the moves.
 * <p>
 * Unless noted otherwise, every method takes the rows in any order, never modifies them,
 * returns ascending rows and throws an {@link IllegalArgumentException} for a row outside
 * the table or a duplicate row.
 *
 * @author Christian Pesch
 */

public class RowMoves {
    private RowMoves() {
    }

    private static int[] checked(int[] rows, int rowCount) {
        int[] sorted = rows.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i] < 0 || sorted[i] >= rowCount)
                throw new IllegalArgumentException("Row " + sorted[i] + " is outside 0.." + (rowCount - 1));
            if (i > 0 && sorted[i] == sorted[i - 1])
                throw new IllegalArgumentException("Row " + sorted[i] + " is given twice");
        }
        return sorted;
    }

    /**
     * Returns the signed number of rows the dragged rows move when they are dropped onto
     * a row (drop mode {@code ON}): negative moves them up, positive moves them down, 0 means
     * no move. All dragged rows move by the same distance, so gaps between them stay.
     * Unlike the other methods it does not check the rows.
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

    /**
     * Returns the rows after moving them up {@code delta} times by one row; a row whose
     * next row is a marked row that could not move stays, so rows stacked at the top stay.
     */
    public static int[] up(int[] rows, int delta) {
        int[] result = checked(rows, MAX_VALUE);
        for (int step = 0; step < delta; step++) {
            int firstFreeRow = 0;
            for (int i = 0; i < result.length; i++) {
                if (result[i] - 1 < firstFreeRow) {
                    firstFreeRow = result[i] + 1;
                    continue;
                }
                result[i]--;
                firstFreeRow = result[i] + 1;
            }
        }
        return result;
    }

    /**
     * Returns the rows after moving them down {@code delta} times by one row; a row whose
     * next row is a marked row that could not move stays, so rows stacked at the bottom stay.
     */
    public static int[] down(int[] rows, int delta, int rowCount) {
        int[] result = checked(rows, rowCount);
        for (int step = 0; step < delta; step++) {
            int lastFreeRow = rowCount - 1;
            for (int i = result.length - 1; i >= 0; i--) {
                if (result[i] + 1 > lastFreeRow) {
                    lastFreeRow = result[i] - 1;
                    continue;
                }
                result[i]++;
                lastFreeRow = result[i] - 1;
            }
        }
        return result;
    }

    /**
     * Returns the rows after moving them to the top: {@code 0..k-1}.
     */
    public static int[] top(int[] rows) {
        int[] result = checked(rows, MAX_VALUE);
        for (int i = 0; i < result.length; i++)
            result[i] = i;
        return result;
    }

    /**
     * Returns the rows after moving them to the bottom: {@code rowCount-k..rowCount-1}.
     */
    public static int[] bottom(int[] rows, int rowCount) {
        int[] result = checked(rows, rowCount);
        for (int i = 0; i < result.length; i++)
            result[i] = rowCount - result.length + i;
        return result;
    }

    /**
     * Returns whether moving the rows up would move any of them.
     */
    public static boolean canMoveUp(int[] rows) {
        return rows.length > 0 && !Arrays.equals(checked(rows, MAX_VALUE), top(rows));
    }

    /**
     * Returns whether moving the rows down would move any of them.
     */
    public static boolean canMoveDown(int[] rows, int rowCount) {
        return rows.length > 0 && !Arrays.equals(checked(rows, rowCount), bottom(rows, rowCount));
    }

    /**
     * Returns the permutation that moves the rows to the moved rows: {@code order[newRow] = oldRow}.
     * The marked rows keep their relative order, the other rows fill the free rows in their old order.
     *
     * @param rowCount  the number of rows of the table
     * @param rows      the marked rows before the move
     * @param movedRows the marked rows after the move, as returned by {@link #up}, {@link #down},
     *                  {@link #top} or {@link #bottom}
     */
    public static int[] order(int rowCount, int[] rows, int[] movedRows) {
        int[] from = checked(rows, rowCount);
        int[] to = checked(movedRows, rowCount);
        if (from.length != to.length)
            throw new IllegalArgumentException("Got " + from.length + " rows but " + to.length + " moved rows");

        int[] order = new int[rowCount];
        boolean[] marked = new boolean[rowCount];
        boolean[] taken = new boolean[rowCount];
        for (int i = 0; i < from.length; i++) {
            order[to[i]] = from[i];
            marked[from[i]] = true;
            taken[to[i]] = true;
        }

        int newRow = 0;
        for (int oldRow = 0; oldRow < rowCount; oldRow++) {
            if (marked[oldRow])
                continue;
            while (taken[newRow])
                newRow++;
            order[newRow++] = oldRow;
        }
        return order;
    }

    /**
     * Returns the permutation that undoes the given permutation.
     */
    public static int[] inverse(int[] order) {
        int[] inverse = new int[order.length];
        boolean[] seen = new boolean[order.length];
        for (int newRow = 0; newRow < order.length; newRow++) {
            int oldRow = order[newRow];
            if (oldRow < 0 || oldRow >= order.length || seen[oldRow])
                throw new IllegalArgumentException("Not a permutation: " + Arrays.toString(order));
            seen[oldRow] = true;
            inverse[oldRow] = newRow;
        }
        return inverse;
    }
}
