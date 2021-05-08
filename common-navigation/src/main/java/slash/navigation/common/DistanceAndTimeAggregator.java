package slash.navigation.common;

import javax.swing.event.EventListenerList;
import java.util.Map;
import java.util.TreeMap;

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
        relativeDistancesAndTimes.put(0, ZERO);
        absoluteDistancesAndTimes.put(0, ZERO);
    }

    public void calculatedDistancesAndTimes(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        this.relativeDistancesAndTimes.putAll(indexToDistanceAndTime);

        int firstIndex = Integer.MAX_VALUE;
        int lastIndex = 0;
        for (Integer index : indexToDistanceAndTime.keySet()) {
            if (index < firstIndex)
                firstIndex = index;
            if (index > lastIndex)
                lastIndex = index;
        }

        updateAbsoluteDistancesAndTimes(firstIndex);

        fireDistancesAndTimesChanged(firstIndex, lastIndex);
    }

    private void updateAbsoluteDistancesAndTimes(int startIndex) {
        DistanceAndTime firstPosition = absoluteDistancesAndTimes.get(startIndex - 1);
        int endIndex = relativeDistancesAndTimes.keySet().size();

        double aggregatedDistance = firstPosition.getDistance();
        long aggregatedTime = firstPosition.getTimeInMillis();
        for(int index = startIndex; index < endIndex; index++) {
            DistanceAndTime distanceAndTime = relativeDistancesAndTimes.get(index);
            if(distanceAndTime != null) {
                Double distance = distanceAndTime.getDistance();
                if (!isEmpty(distance))
                    aggregatedDistance += distance;
                Long time = distanceAndTime.getTimeInMillis();
                if (!isEmpty(time))
                    aggregatedTime += time;
            }
            absoluteDistancesAndTimes.put(index, new DistanceAndTime(aggregatedDistance, aggregatedTime));
        }
    }

    public Map<Integer, DistanceAndTime> getAbsoluteDistancesAndTimes() {
        return absoluteDistancesAndTimes;
    }

    public Map<Integer, DistanceAndTime> getRelativeDistancesAndTimes() {
        return relativeDistancesAndTimes;
    }

    public DistanceAndTime getTotalDistanceAndTime() {
        return absoluteDistancesAndTimes.get(absoluteDistancesAndTimes.size() - 1);
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
