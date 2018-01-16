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

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class QstarzQ1000FormatTest {
    private QstarzQ1000Format format = new QstarzQ1000Format();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HDOP,NSAT (USED/VIEW),DISTANCE,"));
        assertTrue(format.isValidLine("8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,"));
        assertTrue(format.isValidLine("5,T,,,SPS,33.930556,S,115.100000,E,374.0 m,0.0 km/h,0.0,0(0),81318.76 m,"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,"));
        assertTrue(format.isPosition("5,T,,,SPS,33.930556,S,115.100000,E,374.0 m,0.0 km/h,0.0,0(0),81318.76 m,"));

        assertFalse(format.isPosition("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HDOP,NSAT (USED/VIEW),DISTANCE,"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,", null);
        assertDoubleEquals(8.614, position.getLongitude());
        assertDoubleEquals(49.126389, position.getLatitude());
        assertDoubleEquals(245.512, position.getElevation());
        assertDoubleEquals(0.759, position.getSpeed());
        assertDoubleEquals(1.4, position.getHdop());
        assertEquals(Integer.valueOf(8), position.getSatellites());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2010, 12, 28, 23, 1, 43);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
    }
}