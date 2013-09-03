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

package slash.navigation.base;

import slash.common.type.CompactCalendar;

import java.util.List;

import static java.lang.Math.abs;
import static java.lang.System.arraycopy;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Provides {@link NavigationPosition} calculation functionality.
 *
 * @author Christian Pesch, Malte Neumann
 */

public class Positions {
    private static final double DIV_BY_ZERO_AVOIDANCE_OFFSET = 0.000000000001;

    private static int[] douglasPeuckerSimplify(List<? extends NavigationPosition> positions, int from, int to, double threshold) {
        // find the point with the maximum distance
        NavigationPosition pointA = positions.get(from);
        NavigationPosition pointB = positions.get(to);
        int maximumDistanceIndex = -1;
        double maximumDistance = 0.0;
        for (int i = from + 1; i < to; i++) {
            NavigationPosition position = positions.get(i);
            if (position.hasCoordinates()) {
                double distance = abs(position.calculateOrthogonalDistance(pointA, pointB));
                if (distance > maximumDistance) {
                    maximumDistance = distance;
                    maximumDistanceIndex = i;
                }
            }
        }

        // if maximum distance is greater than threshold, recursively simplify
        if ((maximumDistanceIndex != -1) && (maximumDistance > threshold)) {
            int[] res1 = douglasPeuckerSimplify(positions, from, maximumDistanceIndex, threshold);
            int[] res2 = douglasPeuckerSimplify(positions, maximumDistanceIndex, to, threshold);

            int[] result = new int[res1.length - 1 + res2.length];
            arraycopy(res1, 0, result, 0, res1.length - 1);
            arraycopy(res2, 0, result, res1.length - 1, res2.length);
            return result;
        } else
            return new int[]{from, to};
    }

    /**
     * Search the significant positions with the Douglas-Peucker-Algorithm.
     * <p/>
     * http://de.wikipedia.org/wiki/Douglas-Peucker-Algorithmus
     *
     * @param positions the original list of positions
     * @param threshold determines the threshold for significance in meter
     * @return an array of indices to the original list of positions with the significant positions
     */
    public static int[] getSignificantPositions(List<? extends NavigationPosition> positions, double threshold) {
        return douglasPeuckerSimplify(positions, 0, positions.size() - 1, threshold);
    }

    public static CompactCalendar extrapolateTime(NavigationPosition position, NavigationPosition predecessor, NavigationPosition beforePredecessor) {
        if (!predecessor.hasTime() || !beforePredecessor.hasTime())
            return null;

        long timeDelta = abs(beforePredecessor.calculateTime(predecessor));
        Double distanceDelta = beforePredecessor.calculateDistance(predecessor);
        if (isEmpty(distanceDelta))
            return null;

        Double distance = predecessor.calculateDistance(position);
        if (isEmpty(distance))
            return null;

        long time = (long) (predecessor.getTime().getTimeInMillis() + (double) timeDelta * (distance / distanceDelta));
        return fromMillis(time);
    }

    public static CompactCalendar intrapolateTime(NavigationPosition position, NavigationPosition predecessor, NavigationPosition successor) {
        if (!predecessor.hasTime() || !successor.hasTime())
            return null;

        long timeDelta = abs(predecessor.calculateTime(successor));
        Double distanceToPredecessor = predecessor.calculateDistance(position);
        if (isEmpty(distanceToPredecessor))
            return null;

        Double distanceToSuccessor = position.calculateDistance(successor);
        if (isEmpty(distanceToSuccessor))
            return null;

        Double distanceRatio = distanceToPredecessor / (distanceToPredecessor + distanceToSuccessor);

        long time = (long) (predecessor.getTime().getTimeInMillis() + (double) timeDelta * distanceRatio);
        return fromMillis(time);
    }

    public static NavigationPosition center(List<? extends NavigationPosition> positions) {
        NavigationPosition northEast = northEast(positions);
        NavigationPosition southWest = southWest(positions);
        double longitude = (southWest.getLongitude() + northEast.getLongitude() + DIV_BY_ZERO_AVOIDANCE_OFFSET) / 2;
        double latitude = (southWest.getLatitude() + northEast.getLatitude() + DIV_BY_ZERO_AVOIDANCE_OFFSET) / 2;
        CompactCalendar time = null;
        if (northEast.hasTime() && southWest.hasTime()) {
            long millis = northEast.getTime().getTimeInMillis() +
                    (southWest.getTime().getTimeInMillis() - northEast.getTime().getTimeInMillis()) / 2;
            time = fromMillis(millis);
        }
        return asPosition(longitude, latitude, time);
    }

    public static NavigationPosition southWest(List<? extends NavigationPosition> positions) {
        double minimumLongitude = 180.0, minimumLatitude = 180.0;
        CompactCalendar minimumTime = null;
        for (NavigationPosition position : positions) {
            Double longitude = position.getLongitude();
            if (longitude == null)
                continue;
            if (longitude < minimumLongitude)
                minimumLongitude = longitude;
            Double latitude = position.getLatitude();
            if (latitude == null)
                continue;
            if (latitude < minimumLatitude)
                minimumLatitude = latitude;
            CompactCalendar time = position.getTime();
            if (time == null)
                continue;
            if (minimumTime == null || time.before(minimumTime))
                minimumTime = time;
        }
        return asPosition(minimumLongitude, minimumLatitude, minimumTime);
    }

    public static NavigationPosition northEast(List<? extends NavigationPosition> positions) {
        double maximumLongitude = -180.0, maximumLatitude = -180.0;
        CompactCalendar maximumTime = null;
        for (NavigationPosition position : positions) {
            Double longitude = position.getLongitude();
            if (longitude == null)
                continue;
            if (longitude > maximumLongitude)
                maximumLongitude = longitude;
            Double latitude = position.getLatitude();
            if (latitude == null)
                continue;
            if (latitude > maximumLatitude)
                maximumLatitude = latitude;
            CompactCalendar time = position.getTime();
            if (time == null)
                continue;
            if (maximumTime == null || time.after(maximumTime))
                maximumTime = time;
        }
        return asPosition(maximumLongitude, maximumLatitude, maximumTime);
    }

    public static boolean contains(NavigationPosition northEastCorner,
                                   NavigationPosition southWestCorner,
                                   NavigationPosition position) {
        boolean result = position.getLongitude() > southWestCorner.getLongitude();
        result = result && (position.getLongitude() < northEastCorner.getLongitude());
        result = result && (position.getLatitude() > southWestCorner.getLatitude());
        result = result && (position.getLatitude() < northEastCorner.getLatitude());
        return result;
    }

    public static Wgs84Position asPosition(double longitude, double latitude) {
        return asPosition(longitude, latitude, (String)null);
    }

    public static Wgs84Position asPosition(double longitude, double latitude, String comment) {
        return new Wgs84Position(longitude, latitude, null, null, null, comment);
    }

    private static Wgs84Position asPosition(double longitude, double latitude, CompactCalendar time) {
        return new Wgs84Position(longitude, latitude, null, null, time, null);
    }
}
