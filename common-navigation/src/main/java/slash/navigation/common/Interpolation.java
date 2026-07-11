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

package slash.navigation.common;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.floor;

/**
 * Computes interior points on the straight line (Luftlinie) between two positions,
 * one every {@code intervalMetres}, excluding the two endpoints. Interpolation is
 * linear in longitude/latitude: for short legs sampled only for an elevation
 * profile a great-circle path is intentionally not used.
 *
 * @author Christian Pesch
 */
public class Interpolation {
    private Interpolation() {
    }

    /**
     * Returns the interior points on the straight line from
     * ({@code longitude1}, {@code latitude1}) to ({@code longitude2}, {@code latitude2}),
     * one every {@code intervalMetres}, as {@code {longitude, latitude}} pairs. The
     * endpoints are excluded. Returns an empty list when {@code intervalMetres <= 0}
     * or the leg is shorter than two intervals.
     */
    public static List<double[]> interpolate(double longitude1, double latitude1,
                                             double longitude2, double latitude2,
                                             double intervalMetres) {
        List<double[]> result = new ArrayList<>();
        if (intervalMetres <= 0)
            return result;

        double distance = Bearing.calculateBearing(longitude1, latitude1, longitude2, latitude2).getDistance();
        int n = (int) floor(distance / intervalMetres);
        if (n < 2)
            return result;

        for (int i = 1; i < n; i++) {
            double t = (double) i / n;
            result.add(new double[]{longitude1 + t * (longitude2 - longitude1),
                    latitude1 + t * (latitude2 - latitude1)});
        }
        return result;
    }
}
