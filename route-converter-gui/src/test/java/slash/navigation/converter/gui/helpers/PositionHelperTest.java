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
import slash.common.helpers.DateTimeParserException;
import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static java.text.DateFormat.*;
import static java.util.Locale.GERMAN;
import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;
import static slash.common.type.CompactCalendar.*;
import static slash.navigation.converter.gui.helpers.PositionHelper.*;

public class PositionHelperTest {

    private String asDefaultLocaleTime(String germanString) throws ParseException {
        germanString = germanString.replace(" ", ", ");
        DateFormat germanFormat = getDateTimeInstance(SHORT, MEDIUM, GERMAN);
        Date date = germanFormat.parse(germanString);
        DateFormat defaultFormat = getDateTimeInstance(SHORT, MEDIUM);
        return defaultFormat.format(date);
    }

    private CompactCalendar parseDateTime(String stringValue, String timeZonePreference) throws DateTimeParserException {
        Calendar parsed = Transfer.getDateTimeFormat(timeZonePreference).parse(stringValue, null);
        // need result in UTC
        return fromMillis(parsed.getTimeInMillis());
    }

    @Test
    public void testParseTimeUTC() throws Exception {
        CompactCalendar expectedCal = calendar(2010, 9, 18, 3, 13, 33, 0, "UTC");
        CompactCalendar actualCal = parseDateTime(asDefaultLocaleTime("18.09.2010 03:13:33"), "UTC");
        String expected = getDateTimeInstance().format(expectedCal.getTime());
        String actual = getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseTimeLocalTime() throws Exception {
        CompactCalendar expectedCal = calendar(2010, 9, 18, 2, 13, 32, 0, "UTC");
        CompactCalendar actualCal = parseDateTime(asDefaultLocaleTime("18.09.2010 03:13:32"), "GMT+1");
        String expected = getDateTimeInstance().format(expectedCal.getTime());
        String actual = getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    // ---- formatTime ----

    @Test
    public void testFormatTimeNull() {
        assertEquals("?", formatTime(null));
    }

    @Test
    public void testFormatTimeZero() {
        // 0 millis = 00:00:00
        assertEquals("00:00:00", formatTime(fromMillis(0L)));
    }

    @Test
    public void testFormatTimeOneHour() {
        // 3600 seconds = 1 hour
        assertEquals("01:00:00", formatTime(fromMillis(3_600_000L)));
    }

    @Test
    public void testFormatTimeOneMinuteFiveSeconds() {
        // 65 seconds
        assertEquals("00:01:05", formatTime(fromMillis(65_000L)));
    }

    @Test
    public void testFormatTimeFormat() {
        // format should always be HH:MM:SS
        String result = formatTime(fromMillis(3_661_000L));
        assertTrue("should match HH:MM:SS", result.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    // ---- formatSize ----

    @Test
    public void testFormatSizeNull() {
        assertEquals("?", formatSize(null));
    }

    @Test
    public void testFormatSizeBytes() {
        String result = formatSize(500L);
        assertTrue("should contain Bytes", result.contains("Bytes"));
    }

    @Test
    public void testFormatSizeKiloBytes() {
        String result = formatSize(3_000L);
        assertTrue("should contain kByte", result.contains("kByte"));
    }

    @Test
    public void testFormatSizeMegaBytes() {
        String result = formatSize(3_000_000L);
        assertTrue("should contain MByte", result.contains("MByte"));
    }

    @Test
    public void testFormatSizeExactlyTwoKiloByteBoundary() {
        // 2 * 1024 = 2048 -> should be kByte
        String result = formatSize(2049L);
        assertTrue("2049 bytes should display as kByte", result.contains("kByte"));
    }

    // ---- formatDate with explicit timeZone ----

    @Test
    public void testFormatDateNullReturnsQuestionMark() {
        assertEquals("?", formatDate(null, "UTC"));
    }

    @Test
    public void testFormatDateWithTimeZoneProducesNonEmptyString() {
        CompactCalendar cal = fromMillis(0L);
        String result = formatDate(cal, "UTC");
        assertNotNull(result);
        assertFalse("should not be empty", result.isEmpty());
    }

    // ---- extractPressure / extractTemperature / extractHeartBeat ----

    @Test
    public void testExtractPressureReturnsEmptyForNonSensorPosition() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractPressure(position));
    }

    @Test
    public void testExtractTemperatureReturnsEmptyForNonSensorPosition() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractTemperature(position));
    }

    @Test
    public void testExtractHeartBeatReturnsEmptyForNonSensorPosition() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractHeartBeat(position));
    }

    // ---- extractFile ----

    @Test
    public void testExtractFileReturnsNullForNonWgs84Position() {
        slash.navigation.common.SimpleNavigationPosition pos =
                new slash.navigation.common.SimpleNavigationPosition(10.0, 50.0);
        assertNull(extractFile(pos));
    }

    @Test
    public void testExtractFileReturnsNullForWgs84PositionWithoutWaypointType() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertNull(extractFile(position));
    }
}
