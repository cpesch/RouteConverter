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

package slash.navigation;

import slash.navigation.util.CompactCalendar;

import java.util.Calendar;
import java.util.TimeZone;
import java.text.DateFormat;

public abstract class TestCase extends junit.framework.TestCase {
    private static final DateFormat LONG_DATE_TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

    public static void assertNotEquals(Object expected, Object was) {
        assertTrue("expected:<" + expected + "> but was:<" + was + ">", !expected.equals(was));
    }

    protected static void assertByteArrayEquals(byte[] expected, byte[] was) {
        assertEquals(expected.length, was.length);
        for (int i = 0; i < expected.length; i++) {
            byte e = expected[i];
            byte w = was[i];
            assertEquals(e, w);
        }
    }

    public static void assertIntArrayEquals(int[] expected, int[] was) {
        assertEquals(expected.length, was.length);
        for (int i = 0; i < expected.length; i++) {
            int e = expected[i];
            int w = was[i];
            assertEquals(e, w);
        }
    }

    public static void assertEquals(long expected, Long was) {
        assertEquals(new Long(expected), was);
    }

    public static void assertNearBy(double expected, double actual) {
        assertNearBy(expected, actual, 0.000005);
    }

    public static void assertNearBy(double expected, double actual, double offset) {
        if (expected != actual) {
            double expectedPlusOffset = expected * (1.0 + offset);
            if (expectedPlusOffset == 0.0)
                expectedPlusOffset = offset;
            assertTrue(actual + " is not within +" + offset + " of " + expected + " to " + expectedPlusOffset,
                    actual > 0.0 ? actual < expectedPlusOffset : actual > expectedPlusOffset);
            double expectedMinusOffset = expected * (1.0 - offset);
            if (expectedMinusOffset == 0.0)
                expectedMinusOffset = -offset;
            assertTrue(actual + " is not within -" + offset + " of " + expected + " to " + expectedMinusOffset,
                    actual > 0.0 ? actual > expectedMinusOffset : actual < expectedMinusOffset);
        }
    }

    public static void assertCalendarEquals(Calendar expected, Calendar actual) {
        String expectedString = LONG_DATE_TIME_FORMAT.format(expected.getTime());
        String actualString = LONG_DATE_TIME_FORMAT.format(actual.getTime());
        assertEquals(expectedString, actualString);
    }

    public static void assertCalendarEquals(CompactCalendar expected, CompactCalendar actual) {
        String expectedString = LONG_DATE_TIME_FORMAT.format(expected.getTime());
        String actualString = LONG_DATE_TIME_FORMAT.format(actual.getTime());
        assertEquals(expectedString, actualString);
    }

    public static CompactCalendar calendar(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Calendar result = Calendar.getInstance();
        result.set(year, month - 1, day, hour, minute, second);
        result.set(Calendar.MILLISECOND, millisecond);
        return CompactCalendar.fromCalendar(result);
    }

    public static CompactCalendar calendar(int year, int month, int day, int hour, int minute, int second) {
        return calendar(year, month, day, hour, minute, second, 0);
    }

    public static CompactCalendar calendar(long millisecond) {
        return calendar(millisecond, TimeZone.getDefault());
    }

    public static CompactCalendar utcCalendar(long millisecond) {
        return calendar(millisecond, "GMT");
    }

    private static CompactCalendar calendar(long millisecond, String timeZone) {
        return calendar(millisecond, TimeZone.getTimeZone(timeZone));
    }

    private static CompactCalendar calendar(long millisecond, TimeZone zone) {
        Calendar result = Calendar.getInstance(zone);
        result.setTimeInMillis(millisecond);
        result.setLenient(false);
        return CompactCalendar.fromCalendar(result);
    }
}
