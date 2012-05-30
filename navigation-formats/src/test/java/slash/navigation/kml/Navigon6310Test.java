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

package slash.navigation.kml;

import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.text.DateFormat;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class Navigon6310Test {
    private Kml20Format format = new Kml20Format();
    private static final String NAVIGON6310_NAME = " 10:08:18, 509.49 meter ";

    @Test
    public void testParseTime() {
        KmlPosition position = new KmlPosition(null, null, null, null, null, null);
        CompactCalendar expectedCal = calendar(1970, 1, 1, 10, 8, 18);
        format.parseTime(position, NAVIGON6310_NAME, null);
        CompactCalendar actualCal = position.getTime();
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        String actual = DateFormat.getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseTimeWithStartDate() {
        KmlPosition position = new KmlPosition(null, null, null, null, null, null);
        CompactCalendar startDate = calendar(2008, 7, 6, 5, 4, 3);
        CompactCalendar expectedCal = calendar(2008, 7, 6, 10, 8, 18);
        format.parseTime(position, NAVIGON6310_NAME, startDate);
        CompactCalendar actualCal = position.getTime();
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        String actual = DateFormat.getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseElevation() {
        assertDoubleEquals(509.49, format.parseElevation(NAVIGON6310_NAME));
    }
}
