package slash.navigation.converter.gui.mapview;

import org.junit.Test;
import slash.common.io.Transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;
import static slash.common.io.Transfer.ceiling;

public class BaseMapViewTest {
    private static final int MAXIMUM_DIRECTIONS_SEGMENT_LENGTH = 4;

    private List<Integer> createIntervals(int size) {
        List<Integer> result = new ArrayList<Integer>();
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
        assertEquals(Arrays.asList(0, 1), createIntervals(2));
        assertEquals(Arrays.asList(1, 0, 2), createIntervals(3));
        assertEquals(Arrays.asList(1, 2, 0, 3), createIntervals(4));
    }

    @Test
    public void intervalsAboveSegmentLength() {
        assertEquals(Arrays.asList(1, 2, 0, 3, 3, 4), createIntervals(5));
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 3, 5), createIntervals(6));
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 3, 6), createIntervals(7));
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 6, 3, 7), createIntervals(8));
    }

    @Test
    public void intervalsAboveDoubleSegmentLength() {
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 6, 3, 7, 7, 8), createIntervals(9));
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 6, 3, 7, 8, 7, 9), createIntervals(10));
    }
}
