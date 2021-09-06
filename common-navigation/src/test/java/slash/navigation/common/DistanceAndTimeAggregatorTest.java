package slash.navigation.common;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class DistanceAndTimeAggregatorTest {
    private final DistanceAndTimeAggregator aggregator = new DistanceAndTimeAggregator();

    private Map<Integer,DistanceAndTime> populate(int... distances) {
        return populateAt(0, distances);
    }

    private Map<Integer,DistanceAndTime> populateAt(int startIndex, int... distances) {
        HashMap<Integer,DistanceAndTime> result = new HashMap<>();
        for(int i = 0; i < distances.length; i++) {
            result.put(i + startIndex, new DistanceAndTime((double) distances[i], (long)distances[i] * 2));
        }
        return result;
    }

    @Test
    public void testAddDistancesAndTimes() {
        DistancesAndTimesAggregatorListener listener = mock(DistancesAndTimesAggregatorListener.class);
        aggregator.addDistancesAndTimesAggregatorListener(listener);

        aggregator.addDistancesAndTimes(populate());
        verify(listener, never()).distancesAndTimesChanged(anyInt(), anyInt());

        aggregator.addDistancesAndTimes(populateAt(1, 5, 10, 15));
        assertEquals(populate(0, 5, 10, 15), aggregator.getRelativeDistancesAndTimes());
        assertEquals(populate(0, 5, 15, 30), aggregator.getAbsoluteDistancesAndTimes());
        assertEquals(new DistanceAndTime(30.0, 60L), aggregator.getTotalDistanceAndTime());
        verify(listener, times(1)).distancesAndTimesChanged(1, 3);

        aggregator.addDistancesAndTimes(populateAt(2, 30));
        assertEquals(populate(0, 5, 30, 10, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(2, 4);

        aggregator.addDistancesAndTimes(populateAt(1, 40));
        assertEquals(populate(0, 40, 5, 30, 10, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(1, 5);

        aggregator.addDistancesAndTimes(populateAt(6, 50));
        assertEquals(populate(0, 40, 5, 30, 10, 15, 50), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(6, 6);
    }

    @Test
    public void testUpdateDistancesAndTimes() {
        DistancesAndTimesAggregatorListener listener = mock(DistancesAndTimesAggregatorListener.class);
        aggregator.addDistancesAndTimesAggregatorListener(listener);

        aggregator.updateDistancesAndTimes(populateAt(1, 5, 10, 15));
        assertEquals(populate(0, 5, 10, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(1, 3);

        aggregator.updateDistancesAndTimes(populateAt(2, 30));
        assertEquals(populate(0, 5, 30, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(2, 3);

        aggregator.updateDistancesAndTimes(populateAt(1, 40));
        assertEquals(populate(0, 40, 30, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(2)).distancesAndTimesChanged(1, 3);

        aggregator.updateDistancesAndTimes(populateAt(3, 50));
        assertEquals(populate(0, 40, 30, 50), aggregator.getRelativeDistancesAndTimes());
        assertEquals(new DistanceAndTime(120.0, 240L), aggregator.getTotalDistanceAndTime());
        verify(listener, times(1)).distancesAndTimesChanged(3, 3);
    }

    @Test
    public void testRemoveDistancesAndTimes() {
        DistancesAndTimesAggregatorListener listener = mock(DistancesAndTimesAggregatorListener.class);
        aggregator.addDistancesAndTimesAggregatorListener(listener);

        aggregator.updateDistancesAndTimes(populateAt(1, 5, 10, 15, 20));
        assertEquals(populate(0, 5, 10, 15, 20), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(1, 4);

        aggregator.removeDistancesAndTimes(populateAt(2, -1));
        assertEquals(populate(0, 5, 15, 20), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(2, 3);

        aggregator.removeDistancesAndTimes(populateAt(3, -1));
        assertEquals(populate(0, 5, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(3, 2);

        aggregator.removeDistancesAndTimes(populateAt(1, -1));
        assertEquals(populate(0, 15), aggregator.getRelativeDistancesAndTimes());
        verify(listener, times(1)).distancesAndTimesChanged(1, 1);
    }
}
