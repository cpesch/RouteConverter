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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.Wgs84Position;

import java.util.Calendar;
import java.util.List;

/**
 * Provides {@link BaseNavigationPosition} calculation functionality.
 *
 * @author Christian Pesch, Malte Neumann
 */

public class Calculation {
    private static final double DIV_BY_ZERO_AVOIDANCE_OFFSET = 0.000000000001;

    private static int[] douglasPeuckerSimplify(List<? extends BaseNavigationPosition> positions, int from, int to, double threshold) {
        // find the point with the maximum distance
        BaseNavigationPosition pointA = positions.get(from);
        BaseNavigationPosition pointB = positions.get(to);
        int maximumDistanceIndex = -1;
        double maximumDistance = 0.0;
        for (int i = from + 1; i < to; i++) {
            // TODO check hasCoordinates
            double d = Math.abs(positions.get(i).calculateOrthogonalDistance(pointA, pointB));
            if (d > maximumDistance) {
                maximumDistance = d;
                maximumDistanceIndex = i;
            }
        }

        // if max distance is greater than threshold, recursively simplify
        if ((maximumDistanceIndex != -1) && (maximumDistance > threshold)) {
            int[] res1 = douglasPeuckerSimplify(positions, from, maximumDistanceIndex, threshold);
            int[] res2 = douglasPeuckerSimplify(positions, maximumDistanceIndex, to, threshold);

            int[] result = new int[res1.length - 1 + res2.length];
            System.arraycopy(res1, 0, result, 0, res1.length - 1);
            System.arraycopy(res2, 0, result, res1.length - 1, res2.length);
            return result;
        } else
            return new int[]{from, to};
    }

    /**
     * Search the significant positions with Douglas-Peucker-algorithm.
     * 
     * http://de.wikipedia.org/wiki/Douglas-Peucker-Algorithmus
     *
     * @param positions the original list of positions
     * @param threshold determines the threshold for significance in meter
     * @return an array of indices to the original list of positions with the significant positions
     */
    public static int[] getSignificantPositions(List<? extends BaseNavigationPosition> positions, double threshold){
        return douglasPeuckerSimplify(positions, 0, positions.size() - 1, threshold);
    }

    public static Wgs84Position center(List<? extends BaseNavigationPosition> positions) {
        Wgs84Position northEast = getNorthEast(positions);
        Wgs84Position southWest = getSouthWest(positions);
        double longitude = (southWest.getLongitude() + northEast.getLongitude() + DIV_BY_ZERO_AVOIDANCE_OFFSET) / 2;
        double latitude = (southWest.getLatitude() + northEast.getLatitude() + DIV_BY_ZERO_AVOIDANCE_OFFSET) / 2;
        Calendar time = null;
        if (northEast.getTime() != null && southWest.getTime() != null) {
            time = Calendar.getInstance();
            long millis = northEast.getTime().getTimeInMillis() +
                    (southWest.getTime().getTimeInMillis() - northEast.getTime().getTimeInMillis()) / 2;
            time.setTimeInMillis(millis);
        }
        return new Wgs84Position(longitude, latitude, null, null, time, null);
    }

    public static Wgs84Position getNorthEast(List<? extends BaseNavigationPosition> positions) {
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
            Calendar time = position.getTime();
            if(time == null)
                continue;
            if(minimumTime == null || time.before(minimumTime))
                minimumTime = time;
        }
        return new Wgs84Position(minimumLongitude, minimumLatitude, null, null, minimumTime, null);
    }

    public static Wgs84Position getSouthWest(List<? extends BaseNavigationPosition> positions) {
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
            Calendar time = position.getTime();
            if(time == null)
                continue;
            if(maximumTime == null || time.after(maximumTime))
                maximumTime = time;
        }
        return new Wgs84Position(maximumLongitude, maximumLatitude, null, null, maximumTime, null);
    }

    public static Wgs84Position duplicateALittleNorth(BaseNavigationPosition position) {
        Double latitude = position.getLatitude();
        if (latitude != null)
            latitude += 0.001;
        return new Wgs84Position(position.getLongitude(), latitude,
                position.getElevation(), position.getSpeed(), position.getTime(), position.getComment());
    }

    public static  boolean containsPosition(BaseNavigationPosition northEastCorner,
                                            BaseNavigationPosition southWestCorner,
                                            BaseNavigationPosition position){
        boolean result = position.getLongitude() > southWestCorner.getLongitude();
        result = result && (position.getLongitude() < northEastCorner.getLongitude());
        result = result && (position.getLatitude() > southWestCorner.getLatitude());
        result = result && (position.getLatitude() < northEastCorner.getLatitude());
        return result;
    }
}
