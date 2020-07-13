package slash.navigation.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.sort;
import static slash.common.io.Transfer.isEmpty;

/**
 * Aggregates {@link DistanceAndTime}s
 *
 * @author Christian Pesch
 */

public class DistanceAndTimeAggregator {
    public static DistanceAndTime max(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        double maxDistance = 0;
        long maxTime = 0;
        for (DistanceAndTime distanceAndTime : indexToDistanceAndTime.values()) {
            if(distanceAndTime == null)
                continue;

            Double distance = distanceAndTime.getDistance();
            if (!isEmpty(distance) && distance > maxDistance)
                maxDistance = distance;

            Long time = distanceAndTime.getTimeInMillis();
            if (!isEmpty(time) && time > maxTime)
                maxTime = time;
        }
        return new DistanceAndTime(maxDistance, maxTime);
    }

    public static Map<Integer, DistanceAndTime> add(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        Map<Integer, DistanceAndTime> result = new HashMap<>(indexToDistanceAndTime.size());
        double aggregatedDistance = 0.0;
        long aggregatedTime = 0L;
        List<Integer> indices = new ArrayList<>(indexToDistanceAndTime.keySet());
        sort(indices);
        for(Integer index : indices) {
            DistanceAndTime distanceAndTime = indexToDistanceAndTime.get(index);
            if(distanceAndTime != null) {
                Double distance = distanceAndTime.getDistance();
                if (!isEmpty(distance))
                    aggregatedDistance += distance;
                Long time = distanceAndTime.getTimeInMillis();
                if (!isEmpty(time))
                    aggregatedTime += time;
            }
            result.put(index, new DistanceAndTime(aggregatedDistance, aggregatedTime));
        }
        return result;
    }
}
