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

package slash.navigation.util;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Position;

import java.util.Calendar;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.System.arraycopy;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.io.Transfer.isEmpty;

/**
 * Provides {@link BaseNavigationPosition} calculation functionality.
 *
 * @author Christian Pesch, Malte Neumann
 */

public class Positions {
    private static final double DIV_BY_ZERO_AVOIDANCE_OFFSET = 0.000000000001;

    private static int[] douglasPeuckerSimplify(List<? extends BaseNavigationPosition> positions, int from, int to, double threshold) {
        // find the point with the maximum distance
        BaseNavigationPosition pointA = positions.get(from);
        BaseNavigationPosition pointB = positions.get(to);
        int maximumDistanceIndex = -1;
        double maximumDistance = 0.0;
        for (int i = from + 1; i < to; i++) {
            BaseNavigationPosition position = positions.get(i);
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
    public static int[] getSignificantPositions(List<? extends BaseNavigationPosition> positions, double threshold) {
        return douglasPeuckerSimplify(positions, 0, positions.size() - 1, threshold);
    }

    public static CompactCalendar interpolateTime(BaseNavigationPosition position, BaseNavigationPosition previous, BaseNavigationPosition beforePrevious) {
        if (beforePrevious.getTime() == null || previous.getTime() == null)
            return null;

        long previousTime = abs(beforePrevious.getTime().getTimeInMillis() - previous.getTime().getTimeInMillis());
        if (previousTime == 0)
            return null;

        Double previousDistance = beforePrevious.calculateDistance(previous);
        if (isEmpty(previousDistance))
            return null;

        Double distance = previous.calculateDistance(position);
        if (isEmpty(distance))
            return null;

        long time = (long) (previous.getTime().getTimeInMillis() + (double) previousTime * (distance / previousDistance));
        return CompactCalendar.fromMillis(time);
    }

    public static BaseNavigationPosition center(List<? extends BaseNavigationPosition> positions) {
        BaseNavigationPosition northEast = northEast(positions);
        BaseNavigationPosition southWest = southWest(positions);
        double longitude = (southWest.getLongitude() + northEast.getLongitude() + DIV_BY_ZERO_AVOIDANCE_OFFSET) / 2;
        double latitude = (southWest.getLatitude() + northEast.getLatitude() + DIV_BY_ZERO_AVOIDANCE_OFFSET) / 2;
        CompactCalendar time = null;
        if (northEast.getTime() != null && southWest.getTime() != null) {
            long millis = northEast.getTime().getTimeInMillis() +
                    (southWest.getTime().getTimeInMillis() - northEast.getTime().getTimeInMillis()) / 2;
            time = CompactCalendar.fromMillis(millis);
        }
        return asPosition(longitude, latitude, time);
    }

    public static BaseNavigationPosition southWest(List<? extends BaseNavigationPosition> positions) {
        double minimumLongitude = 180.0, minimumLatitude = 180.0;
        Calendar minimumTime = null;
        for (BaseNavigationPosition position : positions) {
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
            Calendar calendar = time.getCalendar();
            if (minimumTime == null || calendar.before(minimumTime))
                minimumTime = calendar;
        }
        return asPosition(minimumLongitude, minimumLatitude,
                minimumTime != null ? fromCalendar(minimumTime) : null);
    }

    public static BaseNavigationPosition northEast(List<? extends BaseNavigationPosition> positions) {
        double maximumLongitude = -180.0, maximumLatitude = -180.0;
        Calendar maximumTime = null;
        for (BaseNavigationPosition position : positions) {
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
            Calendar calendar = time.getCalendar();
            if (maximumTime == null || calendar.after(maximumTime))
                maximumTime = calendar;
        }
        return asPosition(maximumLongitude, maximumLatitude,
                maximumTime != null ? fromCalendar(maximumTime) : null);
    }

    public static boolean contains(BaseNavigationPosition northEastCorner,
                                   BaseNavigationPosition southWestCorner,
                                   BaseNavigationPosition position) {
        boolean result = position.getLongitude() > southWestCorner.getLongitude();
        result = result && (position.getLongitude() < northEastCorner.getLongitude());
        result = result && (position.getLatitude() > southWestCorner.getLatitude());
        result = result && (position.getLatitude() < northEastCorner.getLatitude());
        return result;
    }

    public static Wgs84Position asPosition(double longitude, double latitude) {
        return asPosition(longitude, latitude, null);
    }

    private static Wgs84Position asPosition(double longitude, double latitude, CompactCalendar time) {
        return new Wgs84Position(longitude, latitude, null, null, time, null);
    }
}
