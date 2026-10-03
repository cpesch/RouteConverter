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

import java.util.List;

import static java.lang.Math.max;

/**
 * Sums elevation ascend and descend with a hysteresis threshold: an elevation change
 * counts only once it reaches the threshold relative to the last counted elevation,
 * which suppresses GPS elevation noise.
 *
 * @author Christian Pesch
 */

public final class ElevationSums {
    private ElevationSums() {
    }

    public static double ascend(List<Double> elevations, double threshold) {
        return sum(elevations, threshold)[0];
    }

    public static double descend(List<Double> elevations, double threshold) {
        return sum(elevations, threshold)[1];
    }

    private static double[] sum(List<Double> elevations, double threshold) {
        double minimum = max(threshold, 0.0);
        double ascend = 0.0, descend = 0.0;
        Double reference = null;
        for (Double elevation : elevations) {
            if (elevation == null)
                continue;
            if (reference == null) {
                reference = elevation;
                continue;
            }

            double delta = elevation - reference;
            if (delta >= minimum) {
                ascend += delta;
                reference = elevation;
            } else if (delta <= -minimum) {
                descend -= delta;
                reference = elevation;
            }
        }
        return new double[]{ascend, descend};
    }
}
