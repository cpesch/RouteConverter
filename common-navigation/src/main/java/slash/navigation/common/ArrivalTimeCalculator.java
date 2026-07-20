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

import slash.common.type.CompactCalendar;

import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Calculates each position's arrival time of day from a single departure time
 * and the routing durations already calculated as cumulative milliseconds from
 * the start of the route.
 *
 * @author Christian Pesch
 */

public class ArrivalTimeCalculator {

    /**
     * Calculates the arrival time of day for each position.
     * <p>
     * arrival[i] = departure + cumulativeMillisFromStart[i]. cumulativeMillisFromStart[0]
     * is expected to be 0, so arrival[0] equals departure exactly. Date roll-over across
     * midnight is handled since the result is built with {@link CompactCalendar#fromMillis}.
     *
     * @param departure                 the departure time; must not be null
     * @param cumulativeMillisFromStart the cumulative milliseconds from the start of the
     *                                  route to each position
     * @return the arrival time of day for each position
     * @throws IllegalArgumentException if departure is null
     */
    public static CompactCalendar[] calculateArrivalTimes(CompactCalendar departure, long[] cumulativeMillisFromStart) {
        if (departure == null)
            throw new IllegalArgumentException("departure must not be null");

        CompactCalendar[] arrivalTimes = new CompactCalendar[cumulativeMillisFromStart.length];
        for (int i = 0; i < cumulativeMillisFromStart.length; i++) {
            arrivalTimes[i] = fromMillis(departure.getTimeInMillis() + cumulativeMillisFromStart[i]);
        }
        return arrivalTimes;
    }
}

