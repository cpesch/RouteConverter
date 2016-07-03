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

package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.common.type.CompactCalendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import static java.text.DateFormat.*;
import static java.util.Locale.GERMAN;
import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.converter.gui.helpers.PositionHelper.parseDateTime;

public class PositionHelperTest {

    private String asDefaultLocaleTime(String germanString) throws ParseException {
        DateFormat germanFormat = getDateTimeInstance(SHORT, MEDIUM, GERMAN);
        Date date = germanFormat.parse(germanString);
        DateFormat defaultFormat = getDateTimeInstance(SHORT, MEDIUM);
        return defaultFormat.format(date);
    }

    @Test
    public void testParseTimeUTC() throws ParseException {
        CompactCalendar expectedCal = calendar(2010, 9, 18, 3, 13, 32, 0, "UTC");
        CompactCalendar actualCal = parseDateTime(asDefaultLocaleTime("18.09.2010 03:13:32"), "UTC");
        String expected = getDateTimeInstance().format(expectedCal.getTime());
        String actual = getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseTimeLocalTime() throws ParseException {
        CompactCalendar expectedCal = calendar(2010, 9, 18, 2, 13, 32, 0, "UTC");
        CompactCalendar actualCal = parseDateTime(asDefaultLocaleTime("18.09.2010 03:13:32"), "GMT+1");
        String expected = getDateTimeInstance().format(expectedCal.getTime());
        String actual = getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }
}
