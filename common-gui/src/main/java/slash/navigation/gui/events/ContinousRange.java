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

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static slash.navigation.gui.events.Range.asContinuousMonotonicallyDecreasingRanges;
import static slash.navigation.gui.events.Range.asContinuousMonotonicallyIncreasingRanges;

/**
 * Allows to perform customizable {@link RangeOperation}s on indexed elements
 * with another customizable operation after a continuous range.
 * Used to reduce the number of notifications that Swing UI Models fire.
 *
 * @author Christian Pesch
 * @see RangeOperation
 */

public class ContinousRange {
    private final int[] indices;
    private final RangeOperation operation;

    public ContinousRange(int[] indices, RangeOperation operation) {
        this.indices = indices;
        this.operation = operation;
    }

    public void performMonotonicallyIncreasing() {
        perform(asContinuousMonotonicallyIncreasingRanges(indices));
    }

    public void performMonotonicallyIncreasing(int maximumRangeLength) {
        perform(asContinuousMonotonicallyIncreasingRanges(indices, maximumRangeLength));
    }

    public void performMonotonicallyDecreasing() {
        perform(asContinuousMonotonicallyDecreasingRanges(indices));
    }

    private void perform(List<List<Integer>> ranges) {
        for (List<Integer> range : ranges) {
            for (Integer index : range) {
                operation.performOnIndex(index);
                if (operation.isInterrupted())
                    return;
            }
            if (range.size() == 0)
                continue;
            int firstValue = range.get(0);
            int lastValue = range.get(range.size() - 1);
            int from = min(firstValue, lastValue);
            int to = max(firstValue, lastValue);
            operation.performOnRange(from, to);
            if (operation.isInterrupted())
                return;
        }
    }
}
