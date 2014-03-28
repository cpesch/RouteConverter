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
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class HaicomLoggerFormatTest {
    HaicomLoggerFormat format = new HaicomLoggerFormat();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("1,T,08/12/02,05:40:15,47.91561,N,106.90109,E,1308.4m,97.78,1km/h"));
        assertTrue(format.isValidLine("1,T,08/12/06,07:03:40,47.92121,N,106.90585,E,1340.1m,,0km/h"));
        assertTrue(format.isValidLine("151,T,08/12/02,05:47:32,47.91598,N,106.90421,E,1285.1m,60.13,2.4km/h"));
        assertTrue(format.isValidLine("1,T,,,36.87722,N,111.51194,W,1289.0m,0.0,0.0km/h"));
        assertTrue(format.isValidLine("1,T,,,36.87722,N,111.51194,W,0m,0,0km/h"));
        assertTrue(format.isValidLine("INDEX,RCR,DATE,TIME,LATITUDE,N/S,LONGITUDE,E/W,ALTITUDE,COURSE,SPEED,"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("1,T,08/12/02,05:40:15,47.91561,N,106.90109,E,1308.4m,97.78,1km/h"));
        assertTrue(format.isPosition("1,T,08/12/06,07:03:40,47.92121,N,106.90585,E,1340.1m,,0km/h"));
        assertTrue(format.isPosition("151,T,08/12/02,05:47:32,47.91598,N,106.90421,E,1285.1m,60.13,2.4km/h"));
        assertTrue(format.isPosition("1,T,,,36.87722,N,111.51194,W,1289.0m,0.0,0.0km/h"));        
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("1,T,08/12/02,05:40:15,47.91561,N,106.90109,E,1308.4m,97.78,1km/h", null);
        assertDoubleEquals(47.91561, position.getLatitude());
        assertDoubleEquals(106.90109, position.getLongitude());
        assertDoubleEquals(1308.4, position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2008, 12, 2, 5, 40, 15);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getDescription());
    }

    @Test
    public void testParseSouthWestPosition() {
        Wgs84Position position = format.parsePosition("1,T,08/12/02,05:40:15,47.91561,S,106.90109,W,1308.4m,97.78,1km/h", null);
        assertDoubleEquals(-47.91561, position.getLatitude());
        assertDoubleEquals(-106.90109, position.getLongitude());
    }
}