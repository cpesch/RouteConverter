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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;

public class Iblue747FormatTest {
    private Iblue747Format format = new Iblue747Format();

    @Test
    public void testIsValidLine() {
        assertTrue(format.isValidLine("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HEADING,DISTANCE,"));
        assertTrue(format.isValidLine("3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,"));
        assertTrue(format.isValidLine("3,T,,,SPS,32.151111,S,115.835556,E,0.0 M,0.0 km/h,0.0,3425.47 M,"));
    }

    @Test
    public void testIsPosition() {
        assertTrue(format.isPosition("3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,"));
        assertTrue(format.isPosition("3,T,,,SPS,32.151111,S,115.835556,E,0.0 M,0.0 km/h,0.0,3425.47 M,"));

        assertFalse(format.isPosition("INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HEADING,DISTANCE,"));
    }

    @Test
    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("3656,T,2010/12/09,10:59:05,SPS,28.649061,N,17.896196,W,513.863 M,15.862 km/h,178.240250,34.60 M,", null);
        assertDoubleEquals(-17.896196, position.getLongitude());
        assertDoubleEquals(28.649061, position.getLatitude());
        assertDoubleEquals(513.863, position.getElevation());
        assertDoubleEquals(15.862, position.getSpeed());
        assertDoubleEquals(178.240250, position.getHeading());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        CompactCalendar expectedCal = calendar(2010, 12, 9, 10, 59, 5);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
    }
}