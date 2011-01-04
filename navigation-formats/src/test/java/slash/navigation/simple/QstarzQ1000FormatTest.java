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

import slash.common.io.CompactCalendar;
import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.Wgs84Position;

import java.text.DateFormat;

public class QstarzQ1000FormatTest extends NavigationTestCase {
    QstarzQ1000Format format = new QstarzQ1000Format();

    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HDOP,NSAT (USED/VIEW),DISTANCE,"));
        assertTrue(format.isValidLine("8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,"));
    }

    public void testIsPosition() {
        assertTrue(format.isPosition("8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,"));

        assertFalse(format.isPosition("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HDOP,NSAT (USED/VIEW),DISTANCE,"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,", null);
        assertEquals(8.614, position.getLongitude());
        assertEquals(49.126389, position.getLatitude());
        assertEquals(245.512, position.getElevation());
        assertEquals(0.759, position.getSpeed());
        assertEquals(1.4, position.getHdop());
        assertEquals(new Integer(8), position.getSatellites());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2010, 12, 28, 23, 1, 43);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
    }
}