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
import static slash.navigation.common.UnitSystem.Metric;
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

    // ---- extractHeading / extractHdop ----

    @Test
    public void testExtractHeading() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHeading(212.0);
        assertEquals("212.00\u00B0", extractHeading(position));
    }

    @Test
    public void testExtractHeadingWithZero() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHeading(0.0);
        assertEquals("0.00\u00B0", extractHeading(position));
    }

    @Test
    public void testExtractHeadingReturnsEmptyForNullHeading() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractHeading(position));
    }

    @Test
    public void testExtractHdop() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHdop(1.75);
        assertEquals("1.75", extractHdop(position));
    }

    @Test
    public void testExtractHdopWithHalf() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHdop(0.5);
        assertEquals("0.50", extractHdop(position));
    }

    @Test
    public void testExtractHdopReturnsEmptyForNullHdop() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractHdop(position));
    }

    // ---- extractFixQuality / extractAccelerationX, Y, Z ----

    @Test
    public void testExtractFixQuality() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setFixQuality(4);
        assertEquals("4", extractFixQuality(position));
    }

    @Test
    public void testExtractFixQualityWithZero() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setFixQuality(0);
        assertEquals("0", extractFixQuality(position));
    }

    @Test
    public void testExtractFixQualityReturnsEmptyForNullFixQuality() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractFixQuality(position));
    }

    @Test
    public void testExtractAccelerationXIsSignedWithTwoDecimals() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setAccelerationX(-0.3);
        assertEquals("-0.30", extractAccelerationX(position));
    }

    @Test
    public void testExtractAccelerationXTruncatesExtraDecimals() {
        // same fixed-fraction formatting as heading and hdop: Transfer#formatDoubleAsString
        // pads to and truncates at the requested fraction count, it does not round
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setAccelerationX(1.075);
        assertEquals("1.07", extractAccelerationX(position));
    }

    @Test
    public void testExtractAccelerationXReturnsEmptyForNullAccelerationX() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractAccelerationX(position));
    }

    @Test
    public void testExtractAccelerationY() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setAccelerationY(1.08);
        assertEquals("1.08", extractAccelerationY(position));
    }

    @Test
    public void testExtractAccelerationYReturnsEmptyForNullAccelerationY() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractAccelerationY(position));
    }

    @Test
    public void testExtractAccelerationZ() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setAccelerationZ(-9.81);
        assertEquals("-9.81", extractAccelerationZ(position));
    }

    @Test
    public void testExtractAccelerationZReturnsEmptyForNullAccelerationZ() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractAccelerationZ(position));
    }

    // ---- formatElevation (unit system locked explicitly, not preference-driven) ----

    @Test
    public void testFormatElevation() {
        assertEquals("52.00 m", formatElevation(52.0, Metric));
    }

    @Test
    public void testFormatElevationWithHalf() {
        assertEquals("52.50 m", formatElevation(52.5, Metric));
    }

    @Test
    public void testFormatElevationReturnsEmptyForNullElevation() {
        assertEquals("", formatElevation(null));
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
