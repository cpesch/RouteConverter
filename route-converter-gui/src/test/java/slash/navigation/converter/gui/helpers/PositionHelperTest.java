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
import slash.navigation.nmea.NmeaPosition;

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
        assertEquals("212\u00B0", extractHeading(position));
    }

    @Test
    public void testExtractHeadingWithZero() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHeading(0.0);
        assertEquals("0\u00B0", extractHeading(position));
    }

    @Test
    public void testExtractHeadingWithOneDecimalDigit() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHeading(301.3);
        assertEquals("301.3\u00B0", extractHeading(position));
    }

    @Test
    public void testExtractHeadingWithTwoDecimalDigits() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setHeading(212.34);
        assertEquals("212.34\u00B0", extractHeading(position));
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
    public void testExtractSatellites() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setSatellites(12);
        assertEquals("12", extractSatellites(position));
    }

    @Test
    public void testExtractSatellitesWithZero() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        position.setSatellites(0);
        assertEquals("0", extractSatellites(position));
    }

    @Test
    public void testExtractSatellitesReturnsEmptyForNullSatellites() {
        Wgs84Position position = new Wgs84Position(10.0, 50.0, null, null, null, null);
        assertEquals("", extractSatellites(position));
    }

    // ---- extractHeading / extractHdop / extractFixQuality for NmeaPosition ----

    @Test
    public void testExtractHeadingForNmeaPosition() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        position.setHeading(54.11);
        assertEquals("54.11\u00B0", extractHeading(position));
    }

    @Test
    public void testExtractHeadingReturnsEmptyForNmeaPositionWithNullHeading() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        assertEquals("", extractHeading(position));
    }

    @Test
    public void testExtractHdopForNmeaPosition() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        position.setHdop(0.78);
        assertEquals("0.78", extractHdop(position));
    }

    @Test
    public void testExtractHdopReturnsEmptyForNmeaPositionWithNullHdop() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        assertEquals("", extractHdop(position));
    }

    @Test
    public void testExtractFixQualityForNmeaPosition() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        position.setFixQuality(2);
        assertEquals("2", extractFixQuality(position));
    }

    @Test
    public void testExtractFixQualityReturnsEmptyForNmeaPositionWithNullFixQuality() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        assertEquals("", extractFixQuality(position));
    }

    @Test
    public void testExtractSatellitesForNmeaPosition() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        position.setSatellites(7);
        assertEquals("7", extractSatellites(position));
    }

    @Test
    public void testExtractSatellitesReturnsEmptyForNmeaPositionWithNullSatellites() {
        NmeaPosition position = new NmeaPosition(10.0, 50.0, null, null, null, null);
        assertEquals("", extractSatellites(position));
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
        assertEquals("52 m", formatElevation(52.0, Metric));
    }

    @Test
    public void testFormatElevationWithHalf() {
        assertEquals("52.5 m", formatElevation(52.5, Metric));
    }

    @Test
    public void testFormatElevationWithOneDecimalDigit() {
        assertEquals("17.2 m", formatElevation(17.2, Metric));
    }

    @Test
    public void testFormatElevationWithTwoDecimalDigits() {
        assertEquals("17.23 m", formatElevation(17.23, Metric));
        assertEquals("52.55 m", formatElevation(52.55, Metric));
    }

    @Test
    public void testFormatElevationRoundsRatherThanTruncates() {
        assertEquals("52.55 m", formatElevation(52.554, Metric));
    }

    @Test
    public void testFormatElevationRoundsHalfUp() {
        assertEquals("52.56 m", formatElevation(52.555, Metric));
    }

    @Test
    public void testFormatElevationReturnsEmptyForNullElevation() {
        assertEquals("", formatElevation(null));
    }

    // ---- formatSpeed (unit system locked explicitly, not preference-driven) ----

    @Test
    public void testFormatSpeed() {
        assertEquals("23.4 km/h", formatSpeed(23.4, Metric));
    }

    @Test
    public void testFormatSpeedWithWholeNumber() {
        assertEquals("23.0 km/h", formatSpeed(23.0, Metric));
    }

    @Test
    public void testFormatSpeedRoundsHalfUp() {
        assertEquals("5.3 km/h", formatSpeed(5.25, Metric));
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
