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

public class eiblue747FormatTest extends NavigationTestCase {
    Eiblue747Format format = new Eiblue747Format();

    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HEADING,DISTANCE,"));
        assertTrue(format.isValidLine("3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,"));
    }

    public void testIsPosition() {
        assertTrue(format.isPosition("3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,"));

        assertFalse(format.isPosition("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HEADING,DISTANCE,"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,", null);
        assertEquals(-17.896196, position.getLongitude());
        assertEquals(28.649061, position.getLatitude());
        assertEquals(513.863, position.getElevation());
        assertEquals(15.862, position.getSpeed());
        assertEquals(178.240250, position.getHeading());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2010, 12, 9, 10, 59, 5);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
    }
}