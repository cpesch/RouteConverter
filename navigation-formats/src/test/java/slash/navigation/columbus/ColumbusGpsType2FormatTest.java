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

package slash.navigation.columbus;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.base.ConvertBase.ignoreLocalTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.*;

public class ColumbusGpsType2FormatTest {
    private final ColumbusGpsType2Format format = new ColumbusGpsType2Format();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,PRES,TEMP"));
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18 "));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18,"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18,Description"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18"));
        assertTrue(format.isPosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51"));
        assertTrue(format.isPosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,"));

        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,PRES,TEMP"));
        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING"));
    }

    @Test
    public void testParsePosition() throws Exception {
        ignoreLocalTimeZone(() -> {
            Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,1", new ParserContextImpl<>());
            assertDoubleEquals(119.269951, position.getLongitude());
            assertDoubleEquals(26.099775, position.getLatitude());
            assertDoubleEquals(-71.0, position.getElevation());
            assertDoubleEquals(22.9, position.getSpeed());
            assertDoubleEquals(51.0, position.getHeading());
            assertDoubleEquals(1021.3, position.getPressure());
            assertDoubleEquals(1.0, position.getTemperature());
            assertNull(position.getHeartBeat());
            String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
            CompactCalendar expectedCal = calendar(2016, 3, 25, 15, 20, 59);
            String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
            assertEquals(expected, actual);
            assertEquals(expectedCal, position.getTime());
            assertEquals("Waypoint 17", position.getDescription());
        });
    }

    @Test
    public void testParsePositionWithShanghaiTimezone() {
        boolean useLocalTimeZone = getUseLocalTimeZone();
        String timeZone = getTimeZone();
        try {
            setUseLocalTimeZone(true);
            setTimeZone("Asia/Shanghai");
            Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,1", new ParserContextImpl<>());
            String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
            CompactCalendar expectedCal = calendar(2016, 3, 25, 7, 20, 59);
            String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
            assertEquals(expected, actual);
            assertEquals(expectedCal, position.getTime());
        } finally {
            setUseLocalTimeZone(useLocalTimeZone);
            setTimeZone(timeZone);
        }
    }

    @Test
    public void testParsePositionWithDescription() {
        Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,1,VOX02971", new ParserContextImpl<>());
        assertEquals("VOX02971", position.getDescription());
    }

    @Test
    public void testParsePositionWithNullHeading() {
        Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,", new ParserContextImpl<>());
        assertNull(position.getHeading());
    }

    @Test
    public void testParseTypeBPosition() throws Exception {
        ignoreLocalTimeZone(() -> {
            Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51", new ParserContextImpl<>());
            assertDoubleEquals(119.269951, position.getLongitude());
            assertDoubleEquals(26.099775, position.getLatitude());
            assertDoubleEquals(-71.0, position.getElevation());
            assertDoubleEquals(22.9, position.getSpeed());
            assertDoubleEquals(51.0, position.getHeading());
            assertNull(position.getPressure());
            assertNull(position.getTemperature());
            assertNull(position.getHeartBeat());
            assertEquals("Waypoint 17", position.getDescription());
            String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
            CompactCalendar expectedCal = calendar(2016, 3, 25, 15, 20, 59);
            String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
            assertEquals(expected, actual);
            assertEquals(expectedCal, position.getTime());
        });
    }
}
