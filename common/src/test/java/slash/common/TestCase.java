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

package slash.common;

import slash.common.type.CompactCalendar;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static slash.common.type.CompactCalendar.UTC;

public abstract class TestCase extends junit.framework.TestCase {
    private static final DateFormat LONG_DATE_TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH);

    public static void assertNotEquals(Object expected, Object was) {
        assertNotEquals("expected:<" + expected + "> but was:<" + was + ">", expected, was);
    }

    public static void assertNotEquals(String message, Object expected, Object was) {
        assertTrue(message, !expected.equals(was));
    }

    public static void assertDoubleEquals(double expected, double was) {
        assertEquals(expected, was);
    }

    public static void assertDoubleArrayEquals(double[] expected, double[] was) {
        assertNotNull(was);
        assertEquals(expected.length, was.length);
        for (int i = 0; i < expected.length; i++) {
            double e = expected[i];
            double w = was[i];
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

    public static void assertNearBy(double expected, double actual) {
        assertNearBy(expected, actual, 0.000005);
    }

    public static void assertNearBy(double expected, double actual, double offset) {
        if (expected != actual) {
            double expectedPlusOffset = expected * (1.0 + offset);
            if (expectedPlusOffset == 0.0)
                expectedPlusOffset = offset;
            assertTrue(actual + " is not within +" + offset + " of " + expected + " to " + expectedPlusOffset,
                    actual >= 0.0 ? actual < expectedPlusOffset : actual > expectedPlusOffset);
            double expectedMinusOffset = expected * (1.0 - offset);
            if (expectedMinusOffset == 0.0)
                expectedMinusOffset = -offset;
            assertTrue(actual + " is not within -" + offset + " of " + expectedMinusOffset + " to " + expected,
                    actual >= 0.0 ? actual > expectedMinusOffset : actual < expectedMinusOffset);
        }
    }

    public static void assertCalendarEquals(Calendar expected, Calendar actual) {
        String expectedString = LONG_DATE_TIME_FORMAT.format(expected.getTime());
        String actualString = LONG_DATE_TIME_FORMAT.format(actual.getTime());
        assertEquals(expectedString, actualString);
    }

    public static void assertCalendarEquals(CompactCalendar expected, CompactCalendar actual) {
        assertCalendarEquals(expected.getCalendar(), actual.getCalendar());
    }

    public static CompactCalendar calendar(int year, int month, int day, int hour, int minute, int second, int millisecond, String timeZone) {
        return calendar(year, month, day, hour, minute, second, millisecond, TimeZone.getTimeZone(timeZone));
    }

    @SuppressWarnings("MagicConstant")
    public static CompactCalendar calendar(int year, int month, int day, int hour, int minute, int second, int millisecond, TimeZone timeZone) {
        Calendar result = Calendar.getInstance(timeZone);
        result.set(year, month - 1, day, hour, minute, second);
        result.set(Calendar.MILLISECOND, millisecond);
        return CompactCalendar.fromCalendar(result);
    }

    public static CompactCalendar calendar(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        return calendar(year, month, day, hour, minute, second, millisecond, UTC);
    }

    public static CompactCalendar calendar(int year, int month, int day, int hour, int minute, int second) {
        return calendar(year, month, day, hour, minute, second, 0);
    }

    public static CompactCalendar localCalendar(long millisecond) {
        return calendar(millisecond, TimeZone.getDefault());
    }

    public static CompactCalendar utcCalendar(long millisecond) {
        return calendar(millisecond, UTC);
    }

    private static CompactCalendar calendar(long millisecond, TimeZone timeZone) {
        Calendar result = Calendar.getInstance(timeZone);
        result.setTimeInMillis(millisecond);
        result.setLenient(false);
        return CompactCalendar.fromCalendar(result);
    }
}
