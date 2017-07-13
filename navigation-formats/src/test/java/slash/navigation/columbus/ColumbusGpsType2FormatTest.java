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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.setTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.setUseLocalTimeZone;

public class ColumbusGpsType2FormatTest {
    private ColumbusGpsType2Format format = new ColumbusGpsType2Format();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,PRES,TEMP"));
        assertTrue(format.isValidLine("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18 "));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18,"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51"));
        assertTrue(format.isValidLine("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18,Description"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,18"));
        assertTrue(format.isPosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51"));

        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,PRES,TEMP"));
        assertFalse(format.isPosition("INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING"));
    }

    @Test
    public void testParsePosition() {
        boolean useLocalTimeZone = getUseLocalTimeZone();
        try {
            setUseLocalTimeZone(false);
            Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,1", new ParserContextImpl());
            assertDoubleEquals(119.269951, position.getLongitude());
            assertDoubleEquals(26.099775, position.getLatitude());
            assertDoubleEquals(-71.0, position.getElevation());
            assertDoubleEquals(22.9, position.getSpeed());
            assertDoubleEquals(51.0, position.getHeading());
            assertDoubleEquals(1021.3, position.getPressure());
            assertDoubleEquals(1.0, position.getTemperature());
            String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
            CompactCalendar expectedCal = calendar(2016, 3, 25, 15, 20, 59);
            String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
            assertEquals(expected, actual);
            assertEquals(expectedCal, position.getTime());
            assertEquals("Waypoint 17", position.getDescription());
        } finally {
            setUseLocalTimeZone(useLocalTimeZone);
        }
    }

    @Test
    public void testParsePositionWithShanghaiTimezone() {
        boolean useLocalTimeZone = getUseLocalTimeZone();
        String timeZone = getTimeZone();
        try {
            setUseLocalTimeZone(true);
            setTimeZone("Asia/Shanghai");
            Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,1", new ParserContextImpl());
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
        Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51,1021.3,1,VOX02971", new ParserContextImpl());
        assertEquals("VOX02971", position.getDescription());
    }

    @Test
    public void testParseTypeBPosition() {
        boolean useLocalTimeZone = getUseLocalTimeZone();
        try {
            setUseLocalTimeZone(false);
            Wgs84Position position = format.parsePosition("17,T,160325,152059,26.099775N,119.269951E,-71,22.9,51", new ParserContextImpl());
            assertDoubleEquals(119.269951, position.getLongitude());
            assertDoubleEquals(26.099775, position.getLatitude());
            assertDoubleEquals(-71.0, position.getElevation());
            assertDoubleEquals(22.9, position.getSpeed());
            assertDoubleEquals(51.0, position.getHeading());
            assertNull(position.getPressure());
            assertNull(position.getTemperature());
            assertEquals("Waypoint 17", position.getDescription());
            String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
            CompactCalendar expectedCal = calendar(2016, 3, 25, 15, 20, 59);
            String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
            assertEquals(expected, actual);
            assertEquals(expectedCal, position.getTime());
        } finally {
            setUseLocalTimeZone(useLocalTimeZone);
        }
    }
}