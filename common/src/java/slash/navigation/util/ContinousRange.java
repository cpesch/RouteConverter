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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.util;

import java.util.List;

/**
 * Allows to perform customizable operations on indexed elements
 * with another customizable operation after a continuous range.
 * Use to reduce the number of notifications that Swing UI Models fire.
 *
 * @author Christian Pesch
 */

public class ContinousRange {
    private int[] indices;
    private RangeOperation operation;

    public ContinousRange(int[] indices, RangeOperation operation) {
        this.indices = indices;
        this.operation = operation;
    }

    public void performMonotonicallyIncreasing() {
        perform(Range.asContinuousMonotonicallyIncreasingRanges(indices));
    }

    public void performMonotonicallyDecreasing() {
        perform(Range.asContinuousMonotonicallyDecreasingRanges(indices));
    }

    private void perform(List<List<Integer>> ranges) {
        for (List<Integer> range : ranges) {
            for (Integer index : range) {
                operation.performOnIndex(index);
            }
            int firstValue = range.get(0);
            int lastValue = range.get(range.size() - 1);
            int from = Math.min(firstValue, lastValue);
            int to = Math.max(firstValue, lastValue);
            operation.performOnRange(from, to);
        }
    }

    public interface RangeOperation {
        void performOnIndex(int index);
        void performOnRange(int firstIndex, int lastIndex);
    }
}