/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.common.DegreeFormat.*;

public class DegreeFormatTest {
    @Test
    public void testToDegrees() {
        assertEquals("0.0", Degrees.latitudeToDegrees(0.0));
        assertEquals("12.34567", Degrees.latitudeToDegrees(12.34567));
        assertEquals("-12.34567", Degrees.latitudeToDegrees(-12.34567));

        assertEquals("0.0", Degrees.longitudeToDegrees(0.0));
        assertEquals("12.34567", Degrees.longitudeToDegrees(12.34567));
        assertEquals("-12.34567", Degrees.longitudeToDegrees(-12.34567));
    }

    @Test
    public void testToDegreesMinutes() {
        assertEquals("N 0\u00B0 0.000'", Degrees_Minutes.latitudeToDegrees(0.0));
        assertEquals("N 12\u00B0 20.740'", Degrees_Minutes.latitudeToDegrees(12.34567));
        assertEquals("S 12\u00B0 20.740'", Degrees_Minutes.latitudeToDegrees(-12.34567));

        assertEquals("E 0\u00B0 0.000'", Degrees_Minutes.longitudeToDegrees(0.0));
        assertEquals("E 12\u00B0 20.740'", Degrees_Minutes.longitudeToDegrees(12.34567));
        assertEquals("W 12\u00B0 20.740'", Degrees_Minutes.longitudeToDegrees(-12.34567));
    }

    @Test
    public void testToDegreesMinutesSeconds() {
        assertEquals("N 0\u00B0 0' 0.000\"", Degrees_Minutes_Seconds.latitudeToDegrees(0.0));
        assertEquals("N 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.latitudeToDegrees(12.34567));
        assertEquals("S 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.latitudeToDegrees(-12.34567));

        assertEquals("E 0\u00B0 0' 0.000\"", Degrees_Minutes_Seconds.longitudeToDegrees(0.0));
        assertEquals("E 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.longitudeToDegrees(12.34567));
        assertEquals("W 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.longitudeToDegrees(-12.34567));
    }

    private static final String LONGITUDE_DD_MM = "E 007\u00B0 11.455'";

    @Test(expected = NumberFormatException.class)
    public void testDegreesParseLongitudeFails() {
        Degrees.parseLongitude(LONGITUDE_DD_MM);
    }

    @Test
    public void testDegreesMinutesParseLongitude() {
        assertEquals(7.1909167, Degrees_Minutes.parseLongitude(LONGITUDE_DD_MM), 0.0);
    }

    @Test(expected = NumberFormatException.class)
    public void testDegreesMinutesSecoondsParseLongitudeFails() {
        Degrees_Minutes_Seconds.parseLongitude(LONGITUDE_DD_MM);
    }

    private static final String LATITUDE_DD_MM = "N 51\u00B0 22.478'";

    @Test(expected = NumberFormatException.class)
    public void testDegreesParseLatitudeFails() {
        Degrees.parseLatitude(LATITUDE_DD_MM);
    }

    @Test
    public void testDegreesMinutesParseLatitude() {
        assertEquals(51.3746333, Degrees_Minutes.parseLatitude(LATITUDE_DD_MM), 0.0);
    }

    @Test(expected = NumberFormatException.class)
    public void testDegreesMinutesSecoondsParseLatitudeFails() {
        Degrees_Minutes_Seconds.parseLatitude(LATITUDE_DD_MM);
    }
}
