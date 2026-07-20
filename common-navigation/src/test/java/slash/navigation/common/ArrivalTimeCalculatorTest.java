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

import org.junit.Test;
import slash.common.type.CompactCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Tests {@link ArrivalTimeCalculator}.
 *
 * @author Christian Pesch
 */

public class ArrivalTimeCalculatorTest {

    @Test
    public void calculatesArrivalTimesFromCumulativeMillis() {
        CompactCalendar departure = fromMillis(9 * 3600 * 1000L);
        long[] cumulativeMillisFromStart = {0, 3_600_000, 7_200_000};

        CompactCalendar[] arrivalTimes = ArrivalTimeCalculator.calculateArrivalTimes(departure, cumulativeMillisFromStart);

        assertEquals(3, arrivalTimes.length);
        assertEquals(departure, arrivalTimes[0]);
        assertEquals(fromMillis(10 * 3600 * 1000L), arrivalTimes[1]);
        assertEquals(fromMillis(11 * 3600 * 1000L), arrivalTimes[2]);
    }

    @Test
    public void singlePositionArrivesAtDeparture() {
        CompactCalendar departure = fromMillis(0);

        CompactCalendar[] arrivalTimes = ArrivalTimeCalculator.calculateArrivalTimes(departure, new long[]{0});

        assertEquals(1, arrivalTimes.length);
        assertEquals(departure, arrivalTimes[0]);
    }

    @Test
    public void emptyInputReturnsEmptyArray() {
        CompactCalendar[] arrivalTimes = ArrivalTimeCalculator.calculateArrivalTimes(fromMillis(0), new long[0]);

        assertEquals(0, arrivalTimes.length);
    }

    @Test
    public void departureCrossingMidnightRollsDateForward() {
        long departureMillis = (23 * 3600 + 30 * 60) * 1000L;
        CompactCalendar departure = fromMillis(departureMillis);
        long[] cumulativeMillisFromStart = {0, 3_600_000};

        CompactCalendar[] arrivalTimes = ArrivalTimeCalculator.calculateArrivalTimes(departure, cumulativeMillisFromStart);

        assertEquals(departureMillis, arrivalTimes[0].getTimeInMillis());
        assertEquals(departureMillis + 3_600_000, arrivalTimes[1].getTimeInMillis());
        assertFalse(arrivalTimes[0].sameDay(arrivalTimes[1]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDepartureThrowsIllegalArgumentException() {
        ArrivalTimeCalculator.calculateArrivalTimes(null, new long[]{0});
    }
}

