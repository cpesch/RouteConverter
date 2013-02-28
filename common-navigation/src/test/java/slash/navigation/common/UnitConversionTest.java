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

import org.junit.Ignore;
import org.junit.Test;

import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.common.Orientation.East;
import static slash.navigation.common.Orientation.North;
import static slash.navigation.common.Orientation.South;
import static slash.navigation.common.Orientation.West;
import static slash.navigation.common.UnitConversion.*;

public class UnitConversionTest {
    @Test
    public void testFeetToMeters() {
        assertDoubleEquals(3.048, feetToMeters(10));
    }

    @Test
    public void testMilesToKilometers() {
        assertDoubleEquals(6.2137119223733395, kilometerToStatuteMiles(10));
    }

    @Test
    public void testMetersPerSecondToKilometersPerHour() {
        assertDoubleEquals(3.6, msToKmh(1.0));
        assertDoubleEquals(20.0, kmhToMs(72.0));
    }

    @Test
    public void testDegrees2Nmea() {
        assertEquals(new ValueAndOrientation(4837.4374, North), latitude2nmea(48.6239566));
        assertEquals(new ValueAndOrientation(903.4036, East), longitude2nmea(9.0567266));
        assertEquals(new ValueAndOrientation(5047.5656, South), latitude2nmea(-50.79276));
        assertEquals(new ValueAndOrientation(927.1961, West), longitude2nmea(-9.45327));
    }

    @Test
    public void testNmea2Degrees() {
        assertDoubleEquals(48.6239566, nmea2degrees(new ValueAndOrientation(4837.4374, East)));
        assertDoubleEquals(9.0567266, nmea2degrees(new ValueAndOrientation(903.4036, North)));
        assertDoubleEquals(-50.7927616, nmea2degrees(new ValueAndOrientation(5047.5657, West)));
        assertDoubleEquals(-9.45327, nmea2degrees(new ValueAndOrientation(927.1962, South)));
    }

    @Test
    public void testDegrees2Ddmm() {
        assertEquals("E 9° 3.343'", longitude2ddmm(9.0557233));
        assertEquals("N 48° 37.437'", latitude2ddmm(48.6239566));
    }

    @Test
    @Ignore // TODO
    public void testDdmm2Degrees() {
        assertEquals(9.0557233, ddmm2longitude("E 9° 3.343'"));
        assertEquals(48.6239566, ddmm2latitude("N 48° 37.437'"));
    }

    @Test
    public void testDegrees2Ddmmss() {
        assertEquals("E 9° 3' 20.604\"", longitude2ddmmss(9.0557233));
        assertEquals("N 48° 37' 26.244\"", latitude2ddmmss(48.6239566));
    }

    @Test
    @Ignore // TODO
    public void testDdmmss2Degrees() {
        assertEquals(9.0557233, ddmmss2longitude("E 9° 3' 20.6\""));
        assertEquals(48.6239566, ddmmss2latitude("N 48° 37' 26.2\""));
    }
}
