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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.gopal;

import slash.navigation.NavigationFileParser;
import slash.navigation.NavigationTestCase;
import slash.navigation.Wgs84Position;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;

public class GoPalTrackFormatTest extends NavigationTestCase {
    GoPalTrackFormat format = new GoPalTrackFormat();

    public void testIsValidLine() {
        assertTrue(format.isValidLine("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3"));
        assertTrue(format.isValidLine("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3 "));
        assertTrue(format.isValidLine("6664226,180820,8.016903,52.345550,12.95,30.0394,2,3.000000,3"));
        assertTrue(format.isValidLine("6651145, 180807, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertTrue(format.isValidLine("6122534, 160149, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertTrue(format.isValidLine("54850635, 184229, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));        
    }

    public void testIsPosition() {
        assertTrue(format.isPosition("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3"));
        assertTrue(format.isPosition("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3 "));
        assertTrue(format.isPosition("6664226,180820,8.016903,52.345550,12.95,30.0394,2,3.000000,3"));
        assertFalse(format.isPosition("6651145, 180807, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
        assertFalse(format.isPosition("6122534, 160149, 0.000000, 0.000000, 0, 0, 0, 0.000000, 0"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("6664226, 180820, 8.016903, 52.345550, 12.95, 30.0394, 2, 3.000000, 3");
        assertEquals(8.016903, position.getLongitude());
        assertEquals(52.34555, position.getLatitude());
        assertNull(position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTimeInMillis(position.getTime().getTimeInMillis());
        expectedCal.set(Calendar.HOUR_OF_DAY, 18);
        expectedCal.set(Calendar.MINUTE, 8);
        expectedCal.set(Calendar.SECOND, 20);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getComment());
    }

    public void testParseNegativePosition() {
        Wgs84Position position = format.parsePosition("6664226, 180820, -8.016903, -52.345550, 12.95, 30.0394, 2, 3.000000, 3");
        assertEquals(-8.016903, position.getLongitude());
        assertEquals(-52.34555, position.getLatitude());
        assertNull(position.getElevation());
        String actual = DateFormat.getDateTimeInstance().format(position.getTime().getTime());
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTimeInMillis(position.getTime().getTimeInMillis());
        expectedCal.set(Calendar.HOUR_OF_DAY, 18);
        expectedCal.set(Calendar.MINUTE, 8);
        expectedCal.set(Calendar.SECOND, 20);
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, position.getTime());
        assertNull(position.getComment());
    }

 public void testIsNmn6FavoritesWithValidPositions() throws IOException {
        File source = new File(SAMPLE_PATH + "dieter3-GoPal3Track.trk");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals(GoPalTrackFormat.class, parser.getFormat().getClass());
    }
}
