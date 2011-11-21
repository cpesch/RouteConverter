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

package slash.common.io;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.calendar;
import static slash.common.io.ISO8601.format;
import static slash.common.io.ISO8601.parse;

public class ISO8601Test {

    @Test
    public void testParseGMT() {
        Calendar actual = parse("2007-03-04T14:49:05Z");
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testParseTimeZoneSeparatedByPlus() {
        Calendar actual = parse("2007-03-04T14:49:05+03:00");
        Calendar expected = calendar(2007, 3, 4, 11, 49, 5).getCalendar();
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testParseTimeZoneSeparatedByT() {
        Calendar actual = parse("2007-03-04T14:49:05T03:00");
        Calendar expected = calendar(2007, 3, 4, 11, 49, 5).getCalendar();
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testParseTimeZoneSeparatedByMinus() {
        Calendar actual = parse("2007-03-04T14:49:05-03:00");
        Calendar expected = calendar(2007, 3, 4, 17, 49, 5).getCalendar();
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testFormatGMT() {
        String string = "2007-03-04T14:49:05Z";
        Calendar actual = parse(string);
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        assertEquals(string, format(actual, false));
        assertEquals(format(expected, false), format(actual, false));
        assertEquals(format(expected, true), format(actual, true));
    }

    @Test
    public void testFormatTimeZone() {
        String string = "2007-03-04T14:49:05+03:30";
        Calendar actual = parse(string);
        Calendar expected = calendar(2007, 3, 4, 14, 49, 5).getCalendar();
        String[] ids = TimeZone.getAvailableIDs((3 * 3600 + 30 * 60) * 1000);
        expected.setTimeZone(TimeZone.getTimeZone(ids[0]));
        assertEquals(string, format(actual, false));
    }

    @Test
    public void testFormatWithMilliSeconds1() {
        Calendar actual = parse("2010-09-18T03:13:32.2Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32, 200).getCalendar();
        assertEquals(format(expected, true), format(actual, true));
    }

    @Test
    public void testFormatWithMilliSeconds2() {
        Calendar actual = parse("2010-09-18T03:13:32.29Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32, 290).getCalendar();
        assertEquals(format(expected, true), format(actual, true));
    }

    @Test
    public void testFormatWithMilliSeconds3() {
        Calendar actual = parse("2010-09-18T03:13:32.293Z");
        Calendar expected = calendar(2010, 9, 18, 3, 13, 32, 293).getCalendar();
        assertEquals(format(expected, true), format(actual, true));
    }
}
