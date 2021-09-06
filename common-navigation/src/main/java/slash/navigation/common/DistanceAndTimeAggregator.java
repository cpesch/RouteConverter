package slash.navigation.common;

import javax.swing.event.EventListenerList;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Integer.MAX_VALUE;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.common.DistanceAndTime.ZERO;

/**
 * Aggregates {@link DistanceAndTime}s
 *
 * @author Christian Pesch
 */

public class DistanceAndTimeAggregator {
    private final Map<Integer, DistanceAndTime> relativeDistancesAndTimes = new TreeMap<>();
    private final Map<Integer, DistanceAndTime> absoluteDistancesAndTimes = new TreeMap<>();
    private final EventListenerList listenerList = new EventListenerList();

    public DistanceAndTimeAggregator() {
        initialize();
    }

    private void initialize() {
        relativeDistancesAndTimes.clear();
        relativeDistancesAndTimes.put(0, ZERO);
        absoluteDistancesAndTimes.clear();
        absoluteDistancesAndTimes.put(0, ZERO);
    }

    public synchronized void addDistancesAndTimes(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        FirstAndLastIndex firstAndLastIndex = calculateFirstAndLastIndex(indexToDistanceAndTime);
        if(firstAndLastIndex == null)
            return;

        int diff = firstAndLastIndex.lastIndex - firstAndLastIndex.firstIndex + 1;
        for (int i = relativeDistancesAndTimes.size() - 1; i >= firstAndLastIndex.lastIndex - 1; i--) {
            DistanceAndTime move = relativeDistancesAndTimes.get(i);
            int moveIndex = i + diff;
            relativeDistancesAndTimes.put(moveIndex, new DistanceAndTime(move.getDistance(), move.getTimeInMillis()));
        }
        relativeDistancesAndTimes.putAll(indexToDistanceAndTime);
        updateAbsoluteDistancesAndTimes(firstAndLastIndex.firstIndex);
        fireDistancesAndTimesChanged(firstAndLastIndex.firstIndex, getLastIndexForEvents());
    }

    // everything after lastIndex must be updated, too, for distance and time
    // avoiding to use Integer.MAX_VALUE since JTable clears the selection
    private int getLastIndexForEvents() {
        return absoluteDistancesAndTimes.size() - 1;
    }

    public synchronized void updateDistancesAndTimes(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        FirstAndLastIndex firstAndLastIndex = calculateFirstAndLastIndex(indexToDistanceAndTime);
        if(firstAndLastIndex == null)
            return;

        relativeDistancesAndTimes.putAll(indexToDistanceAndTime);
        updateAbsoluteDistancesAndTimes(firstAndLastIndex.firstIndex);
        fireDistancesAndTimesChanged(firstAndLastIndex.firstIndex, getLastIndexForEvents());
    }

    public synchronized void removeDistancesAndTimes(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        FirstAndLastIndex firstAndLastIndex = calculateFirstAndLastIndex(indexToDistanceAndTime);
        if(firstAndLastIndex == null)
            return;

        int sizeBeforeRemoval = relativeDistancesAndTimes.size();
        for (int i = firstAndLastIndex.firstIndex; i <= firstAndLastIndex.lastIndex; i++) {
            relativeDistancesAndTimes.remove(i);
            absoluteDistancesAndTimes.remove(i);
        }
        for (int i = firstAndLastIndex.lastIndex + 1; i < sizeBeforeRemoval; i++) {
            DistanceAndTime move = relativeDistancesAndTimes.remove(i);
            int moveIndex = firstAndLastIndex.firstIndex + i - (firstAndLastIndex.lastIndex + 1);
            relativeDistancesAndTimes.put(moveIndex, new DistanceAndTime(move.getDistance(), move.getTimeInMillis()));
            absoluteDistancesAndTimes.remove(i);
        }

        updateAbsoluteDistancesAndTimes(firstAndLastIndex.firstIndex);
        fireDistancesAndTimesChanged(firstAndLastIndex.firstIndex, getLastIndexForEvents());
    }

    public synchronized void clearDistancesAndTimes() {
        initialize();
        fireDistancesAndTimesChanged(0, MAX_VALUE);
    }

    private void updateAbsoluteDistancesAndTimes(int startIndex) {
        DistanceAndTime firstPosition = absoluteDistancesAndTimes.get(startIndex - 1);
        if(firstPosition == null)
            return;
        int endIndex = relativeDistancesAndTimes.keySet().size();

        double aggregatedDistance = firstPosition.getDistance();
        long aggregatedTime = firstPosition.getTimeInMillis();
        for (int index = startIndex; index < endIndex; index++) {
            DistanceAndTime distanceAndTime = relativeDistancesAndTimes.get(index);
            if(distanceAndTime == null)
                continue;
            Double distance = distanceAndTime.getDistance();
            if (!isEmpty(distance))
                aggregatedDistance += distance;
            Long time = distanceAndTime.getTimeInMillis();
            if (!isEmpty(time))
                aggregatedTime += time;
            absoluteDistancesAndTimes.put(index, new DistanceAndTime(aggregatedDistance, aggregatedTime));
        }
    }

    private static class FirstAndLastIndex {
        public int firstIndex;
        public int lastIndex;

        public FirstAndLastIndex(int firstIndex, int lastIndex) {
            this.firstIndex = firstIndex;
            this.lastIndex = lastIndex;
        }
    }

    private FirstAndLastIndex calculateFirstAndLastIndex(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        int firstIndex = MAX_VALUE;
        int lastIndex = 0;
        for (Integer index : indexToDistanceAndTime.keySet()) {
            if (index < firstIndex)
                firstIndex = index;
            if (index > lastIndex)
                lastIndex = index;
        }
        return firstIndex != MAX_VALUE ? new FirstAndLastIndex(firstIndex, lastIndex): null;
    }

    public Map<Integer, DistanceAndTime> getAbsoluteDistancesAndTimes() {
        return absoluteDistancesAndTimes;
    }

    public Map<Integer, DistanceAndTime> getRelativeDistancesAndTimes() {
        return relativeDistancesAndTimes;
    }

    public DistanceAndTime getTotalDistanceAndTime() {
        DistanceAndTime total = absoluteDistancesAndTimes.get(absoluteDistancesAndTimes.size() - 1);
        return total != null ? total : ZERO;
    }

    private void fireDistancesAndTimesChanged(int firstIndex, int lastIndex) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == DistancesAndTimesAggregatorListener.class) {
                ((DistancesAndTimesAggregatorListener) listeners[i + 1]).distancesAndTimesChanged(firstIndex, lastIndex);
            }
        }
    }

    public void addDistancesAndTimesAggregatorListener(DistancesAndTimesAggregatorListener l) {
        listenerList.add(DistancesAndTimesAggregatorListener.class, l);
    }
}
