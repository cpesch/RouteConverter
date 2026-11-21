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

package slash.navigation.garmin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Checks the two rounding-sensitive fields of the "W" line, "%5.f" (altitude) and "%5e" (proximity),
 * against what the C library's printf emits for the same inputs. The expected strings were captured
 * once from printf, they are not derived from the Java formatter.
 *
 * @author Christian Pesch
 */

public class GarminPcx5FormatStringIT {
    private static void assertAltitude(String expected, double altitude) {
        assertEquals("altitude " + altitude, expected, GarminPcx5Format.formatFixed(altitude, 5, 0, false));
    }

    @Test
    public void unknownAltitude() {
        assertAltitude("-9999", -9999.0);
    }

    @Test
    public void altitudeWithFractionalPartOfOneHalf() {
        assertAltitude("    2", 2.5);
        assertAltitude("    4", 3.5);
        assertAltitude("    0", 0.5);
        assertAltitude("   -0", -0.5);
        assertAltitude(" 1234", 1234.5);
        assertAltitude("-1234", -1234.5);
        assertAltitude("100000", 99999.5);
        assertAltitude("  100", 100.5);
    }

    @Test
    public void altitudeZero() {
        assertAltitude("    0", 0.0);
    }

    @Test
    public void proximity() {
        assertEquals("0.000000e+00", GarminPcx5Format.formatProximity());
    }

    @Test
    public void coordinates() {
        assertEquals("00030.00000", GarminPcx5Format.formatFixed(30.0, 11, 5, true));
        assertEquals("30.00000", GarminPcx5Format.formatFixed(30.0, 8, 5, true));
    }
}
