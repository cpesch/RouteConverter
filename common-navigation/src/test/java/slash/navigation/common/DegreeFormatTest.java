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
    public void testDegrees() {
        assertEquals("0.0", Degrees.latitudeToDegrees(0.0));
        assertEquals("12.34567", Degrees.latitudeToDegrees(12.34567));
        assertEquals("-12.34567", Degrees.latitudeToDegrees(-12.34567));

        assertEquals("0.0", Degrees.longitudeToDegrees(0.0));
        assertEquals("12.34567", Degrees.longitudeToDegrees(12.34567));
        assertEquals("-12.34567", Degrees.longitudeToDegrees(-12.34567));
    }

    @Test
    public void testDegreesMinutes() {
        assertEquals("N 0\u00B0 0.000'", Degrees_Minutes.latitudeToDegrees(0.0));
        assertEquals("N 12\u00B0 20.740'", Degrees_Minutes.latitudeToDegrees(12.34567));
        assertEquals("S 12\u00B0 20.740'", Degrees_Minutes.latitudeToDegrees(-12.34567));

        assertEquals("E 0\u00B0 0.000'", Degrees_Minutes.longitudeToDegrees(0.0));
        assertEquals("E 12\u00B0 20.740'", Degrees_Minutes.longitudeToDegrees(12.34567));
        assertEquals("W 12\u00B0 20.740'", Degrees_Minutes.longitudeToDegrees(-12.34567));
    }

    @Test
    public void testDegreesMinutesSeconds() {
        assertEquals("N 0\u00B0 0' 0.000\"", Degrees_Minutes_Seconds.latitudeToDegrees(0.0));
        assertEquals("N 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.latitudeToDegrees(12.34567));
        assertEquals("S 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.latitudeToDegrees(-12.34567));

        assertEquals("E 0\u00B0 0' 0.000\"", Degrees_Minutes_Seconds.longitudeToDegrees(0.0));
        assertEquals("E 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.longitudeToDegrees(12.34567));
        assertEquals("W 12\u00B0 20' 44.412\"", Degrees_Minutes_Seconds.longitudeToDegrees(-12.34567));
    }
}
