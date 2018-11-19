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

package slash.navigation.gopal;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;
import java.util.Calendar;

import static java.util.Calendar.*;
import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.common.type.CompactCalendar.UTC;

public class GoPalTrackFormatTest {
    GoPalTrackFormat format = new GoPalTrackFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3"));
        assertTrue(format.isValidLine("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3 "));
        assertTrue(format.isValidLine("6664226,180820,8.016903,52.345550,12.95,30.0394,2,3.000000,3"));
        assertTrue(format.isValidLine("6651145, 180807, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertTrue(format.isValidLine("6122534, 160149, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertTrue(format.isValidLine("54850635, 184229, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertTrue(format.isValidLine("31653, 092258, -22.760357, 65.125717, 334.4, 20.7424, 2, 1.000000, 8, 20100719, 0, 14"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3"));
        assertTrue(format.isPosition("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3 "));
        assertTrue(format.isPosition("6664226,180820,8.016903,52.345550,12.95,30.0394,2,3.000000,3"));
        assertTrue(format.isPosition("31653, 092258, -22.760357, 65.125717, 334.4, 20.7424, 2, 1.000000, 8, 20100719, 0, 14"));
        assertTrue(format.isPosition("31653, 092258, -22.760357, 65.125717, 334.4, 20.7424, 2, 1.000000, 8, 20100719, 0, -14"));
        assertFalse(format.isPosition("6651145, 180807, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertFalse(format.isPosition("6122534, 160149, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000001, 4", new ParserContextImpl());
        assertDoubleEquals(8.016903, position.getLongitude());
        assertDoubleEquals(52.34555, position.getLatitude());
        assertNull(position.getElevation());
        assertDoubleEquals(12.95, position.getHeading());
        assertDoubleEquals(30.0394, position.getSpeed());
        assertDoubleEquals(3.000001, position.getHdop());
        assertEquals(Integer.valueOf(4), position.getSatellites());
        DateFormat format = DateFormat.getDateTimeInstance();
        format.setTimeZone(UTC);
        String actual = format.format(position.getTime().getTime());
        Calendar expectedCal = Calendar.getInstance(UTC);
        expectedCal.setTimeInMillis(position.getTime().getTimeInMillis());
        expectedCal.set(HOUR_OF_DAY, 18);
        expectedCal.set(MINUTE, 8);
        expectedCal.set(SECOND, 20);
        String expected = format.format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime().getCalendar());
        assertNull(position.getDescription());
    }

    @Test
    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("6664226, 180820, -8.016903, -52.345550, 12.95, 30.0394, 2, 3.000000, 3", new ParserContextImpl());
        assertDoubleEquals(-8.016903, position.getLongitude());
        assertDoubleEquals(-52.34555, position.getLatitude());
        assertNull(position.getElevation());
        DateFormat format = DateFormat.getDateTimeInstance();
        format.setTimeZone(UTC);
        String actual = format.format(position.getTime().getTime());
        Calendar expectedCal = Calendar.getInstance(UTC);
        expectedCal.setTimeInMillis(position.getTime().getTimeInMillis());
        expectedCal.set(HOUR_OF_DAY, 18);
        expectedCal.set(MINUTE, 8);
        expectedCal.set(SECOND, 20);
        String expected = format.format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime().getCalendar());
        assertNull(position.getDescription());
    }

    @Test
    public void testParsePositionWithDate() {
        Wgs84Position position = format.parsePosition("31653, 092258, -22.760357, 65.125717, 334.4, 20.7424, 2, 1.000000, 8, 20100719, 0, 14", new ParserContextImpl());
        assertDoubleEquals(-22.760357, position.getLongitude());
        assertDoubleEquals(65.125717, position.getLatitude());
        assertNull(position.getElevation());
        assertDoubleEquals(334.4, position.getHeading());
        assertDoubleEquals(20.7424, position.getSpeed());
        assertDoubleEquals(1.000000, position.getHdop());
        assertEquals(Integer.valueOf(8), position.getSatellites());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2010, 7, 19, 9, 22, 58);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertNull(position.getDescription());
    }
}
