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

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.sort;
import static slash.common.io.Transfer.toArray;

/**
 * Provides Python range() like functionality.
 *
 * @author Christian Pesch
 */

public class Range {
    public static int[] asRange(int firstIndex, int lastIndex) {
        int count = lastIndex - firstIndex + 1;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = firstIndex + i;
        }
        return result;
    }

    public static List<List<Integer>> asContinuousMonotonicallyIncreasingRanges(int[] indices) {
        return asContinuousMonotonicallyIncreasingRanges(indices, MAX_VALUE);
    }

    public static List<List<Integer>> asContinuousMonotonicallyIncreasingRanges(int[] indices, int maximumRangeLength) {
        sort(indices);
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> range = new ArrayList<>();
        for (int index : indices) {
            if ((range.size() == 0 || index == range.get(range.size() - 1) + 1) && range.size() < maximumRangeLength) {
                range.add(index);
            } else {
                result.add(range);
                range = new ArrayList<>();
                range.add(index);
            }
        }
        result.add(range);
        return result;
    }

    public static List<List<Integer>> asContinuousMonotonicallyDecreasingRanges(int[] indices) {
        indices = revert(indices);
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> range = new ArrayList<>();
        for (int index : indices) {
            if (range.size() == 0 || index == range.get(range.size() - 1) - 1) {
                range.add(index);
            } else {
                result.add(range);
                range = new ArrayList<>();
                range.add(index);
            }
        }
        result.add(range);
        return result;
    }

    public static int[] revert(int[] indices) {
        sort(indices);
        int[] reverted = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            reverted[i] = indices[indices.length - i - 1];
        }
        return reverted;
    }

    public static int[] increment(int[] indices, int delta) {
        int[] incremented = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            incremented[i] = indices[i] + delta;
        }
        return incremented;
    }

    public static int[] allButEveryNthAndFirstAndLast(int maximum, int nth) {
        if (nth < 1)
            throw new IllegalArgumentException("nth has to be more than zero");

        List<Integer> result = new ArrayList<>();
        for (int i = 1; i < maximum; i += nth) {
            int intervalMaximum = i + nth - 1;
            if (intervalMaximum > maximum)
                intervalMaximum = maximum;
            for (int j = i; j < intervalMaximum; j++) {
                result.add(j);
            }
        }
        return toArray(result);
    }
}
