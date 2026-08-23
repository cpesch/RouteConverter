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
import slash.navigation.common.NavigationPosition;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Provides {@link NavigationPosition} calculation functionality.
 *
 * @author Christian Pesch, Malte Neumann
 */

public class RouteCalculations {
    // mean earth radius in meters, used to project positions into a local planar
    // frame for the cheap orthogonal distance below; deliberately not Bearing.EARTH_RADIUS
    // (the WGS84 equatorial radius), which serves the exact spherical calculations elsewhere
    private static final double EARTH_RADIUS = 6371000.0;

    private static int[] douglasPeuckerSimplify(List<? extends NavigationPosition> positions, double threshold) throws InterruptedException {
        int positionCount = positions.size();

        // project every position once into a local metric (x, y) frame so that the
        // per-candidate distance below is cheap arithmetic instead of a spherical bearing;
        // longitude is scaled by cos(latitude0) once for the whole track, so a track spanning
        // many degrees of latitude projects slightly distorted (accepted in spec #109)
        double[] x = new double[positionCount];
        double[] y = new double[positionCount];
        boolean[] hasCoordinates = new boolean[positionCount];

        Double latitude0 = null, longitude0 = null;
        for (NavigationPosition position : positions) {
            if (position.hasCoordinates()) {
                latitude0 = position.getLatitude();
                longitude0 = position.getLongitude();
                break;
            }
        }

        if (latitude0 != null) {
            double cosLatitude0 = cos(toRadians(latitude0));
            for (int i = 0; i < positionCount; i++) {
                NavigationPosition position = positions.get(i);
                if (position.hasCoordinates()) {
                    hasCoordinates[i] = true;
                    // longitude delta relative to a fixed reference point, normalized to
                    // [-180, 180), so tracks crossing the +/-180 degree antimeridian project
                    // to nearby x values instead of jumping by ~2*R*cos(lat0)*pi
                    double deltaLongitude = position.getLongitude() - longitude0;
                    deltaLongitude = ((deltaLongitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
                    x[i] = toRadians(deltaLongitude) * cosLatitude0 * EARTH_RADIUS;
                    y[i] = toRadians(position.getLatitude()) * EARTH_RADIUS;
                }
            }
        }

        boolean[] keep = new boolean[positionCount];
        keep[0] = true;
        keep[positionCount - 1] = true;

        Deque<int[]> segments = new ArrayDeque<>();
        segments.push(new int[]{0, positionCount - 1});

        while (!segments.isEmpty()) {
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();

            int[] segment = segments.pop();
            int from = segment[0];
            int to = segment[1];

            // a segment whose endpoints lack coordinates can't evaluate any candidate
            // (matches calculateOrthogonalDistance's null result when pointA/pointB have none)
            if (!hasCoordinates[from] || !hasCoordinates[to])
                continue;

            double ax = x[from], ay = y[from];
            double dx = x[to] - ax, dy = y[to] - ay;
            double len = sqrt(dx * dx + dy * dy);

            // find the point with the maximum distance
            int maximumDistanceIndex = -1;
            double maximumDistance = 0.0;
            for (int i = from + 1; i < to; i++) {
                if (!hasCoordinates[i])
                    continue;

                double px = x[i] - ax, py = y[i] - ay;
                double distance = len == 0.0 ? sqrt(px * px + py * py) : abs(px * dy - py * dx) / len;
                if (distance > maximumDistance) {
                    maximumDistance = distance;
                    maximumDistanceIndex = i;
                }
            }

            // if maximum distance is greater than threshold, split and simplify both halves
            if (maximumDistanceIndex != -1 && maximumDistance > threshold) {
                keep[maximumDistanceIndex] = true;
                segments.push(new int[]{from, maximumDistanceIndex});
                segments.push(new int[]{maximumDistanceIndex, to});
            }
        }

        int keptCount = 0;
        for (boolean k : keep)
            if (k)
                keptCount++;

        int[] result = new int[keptCount];
        int index = 0;
        for (int i = 0; i < positionCount; i++)
            if (keep[i])
                result[index++] = i;
        return result;
    }

    /**
     * Search the significant positions with the Douglas-Peucker-Algorithm.
     *
     * http://de.wikipedia.org/wiki/Douglas-Peucker-Algorithmus
     *
     * @param positions the original list of positions
     * @param threshold determines the threshold for significance in meter
     * @return an array of indices to the original list of positions with the significant positions
     * @throws InterruptedException if the calling thread is interrupted while simplifying
     */
    public static int[] getSignificantPositions(List<? extends NavigationPosition> positions, double threshold) throws InterruptedException {
        if (positions.isEmpty())
            return new int[0];
        else if (positions.size() == 1)
            return new int[]{0};
        else
            return douglasPeuckerSimplify(positions, threshold);
    }

    /**
     * Complement of {@link #getSignificantPositions}: the indices of positions that
     * Douglas-Peucker did not keep.
     *
     * @param positions the original list of positions; the caller is responsible for
     *                   supplying a list that won't be structurally mutated by another
     *                   thread while this runs (e.g. a defensive copy taken on the EDT)
     * @param threshold determines the threshold for significance in meter
     * @return an array of indices to the original list of positions with the insignificant positions
     * @throws InterruptedException if the calling thread is interrupted while simplifying
     */
    public static int[] getInsignificantPositions(List<? extends NavigationPosition> positions, double threshold) throws InterruptedException {
        int positionCount = positions.size();
        int[] significantPositions = getSignificantPositions(positions, threshold);
        BitSet bitset = new BitSet(positionCount);
        for (int significantPosition : significantPositions)
            bitset.set(significantPosition);

        int[] result = new int[positionCount - significantPositions.length];
        int index = 0;
        for (int i = 0; i < positionCount; i++)
            if (!bitset.get(i))
                result[index++] = i;
        return result;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public static CompactCalendar interpolateTime(NavigationPosition position, NavigationPosition predecessor, NavigationPosition successor) {
        if (!predecessor.hasTime() || !successor.hasTime())
            return null;

        long timeDelta = abs(predecessor.calculateTime(successor));
        Double distanceToPredecessor = predecessor.calculateDistance(position);
        if (isEmpty(distanceToPredecessor))
            return null;

        Double distanceToSuccessor = position.calculateDistance(successor);
        if (isEmpty(distanceToSuccessor))
            return null;

        double distanceRatio = distanceToPredecessor / (distanceToPredecessor + distanceToSuccessor);

        long time = (long) (predecessor.getTime().getTimeInMillis() + (double) timeDelta * distanceRatio);
        return fromMillis(time);
    }

    public static Wgs84Position asWgs84Position(Double longitude, Double latitude) {
        return asWgs84Position(longitude, latitude, null);
    }

    public static Wgs84Position asWgs84Position(Double longitude, Double latitude, String description) {
        return new Wgs84Position(longitude, latitude, null, null, null, description);
    }
}
