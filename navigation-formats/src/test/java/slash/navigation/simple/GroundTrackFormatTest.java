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
package slash.navigation.simple;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.util.Calendar.*;
import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.type.CompactCalendar.UTC;

public class GroundTrackFormatTest {
    GroundTrackFormat format = new GroundTrackFormat();

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("  943    52.17661      8.06995     -0.416      0.07569 09:19:58.480"));
        assertTrue(format.isPosition("   83    52.73522      9.88576   4508.976      0.04336 17:01:43.800"));
        assertTrue(format.isPosition("  255    52.23754     13.11184     -0.416      0.00377 -9:-47:-35.236"));
        assertTrue(format.isPosition("  158    52.04482      8.43606  13110.903      0.26909 12:13:40"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("   83    52.73522      9.88576   4508.976      0.04336 17:01:43.800", new ParserContextImpl());
        assertDoubleEquals(9.88576, position.getLongitude());
        assertDoubleEquals(52.73522, position.getLatitude());
        assertDoubleEquals(4508.976, position.getElevation());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        String actual = dateFormat.format(position.getTime().getTime());
        Calendar expectedCal = Calendar.getInstance(UTC);
        expectedCal.setTimeInMillis(position.getTime().getTimeInMillis());
        expectedCal.set(HOUR_OF_DAY, 17);
        expectedCal.set(MINUTE, 1);
        expectedCal.set(SECOND, 43);
        expectedCal.set(MILLISECOND, 800);
        String expected = dateFormat.format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(CompactCalendar.fromCalendar(expectedCal), position.getTime());
        assertEquals("83", position.getDescription());
    }

    @Test
    public void testParsePositionWithInvalidTime() {
        Wgs84Position position = format.parsePosition("   83    52.73522      9.88576   4508.976      0.04336 -17:-01:-43.800", new ParserContextImpl());
        assertNull(position.getTime());
    }

    @Test
    public void testParsePositionWithoutMilliseconds() {
        Wgs84Position position = format.parsePosition("  158    52.04482      8.43606  13110.903      0.26909 12:13:40", new ParserContextImpl());
        assertDoubleEquals(8.43606, position.getLongitude());
        assertDoubleEquals(52.04482, position.getLatitude());
        assertDoubleEquals(13110.903, position.getElevation());
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String actual = dateFormat.format(position.getTime().getTime());
        Calendar expectedCal = Calendar.getInstance(UTC);
        expectedCal.setTimeInMillis(position.getTime().getTimeInMillis());
        expectedCal.set(HOUR_OF_DAY, 12);
        expectedCal.set(MINUTE, 13);
        expectedCal.set(SECOND, 40);
        expectedCal.set(MILLISECOND, 0);
        String expected = dateFormat.format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(CompactCalendar.fromCalendar(expectedCal), position.getTime());
        assertEquals("158", position.getDescription());
    }

}