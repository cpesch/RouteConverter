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

import static org.junit.Assert.assertNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.common.Orientation.*;
import static slash.navigation.common.UnitConversion.*;

public class UnitConversionTest {
    @Test
    public void testFeetToMeters() {
        assertDoubleEquals(3.048, feetToMeters(10));
    }

    @Test
    public void testMilesToKiloMeters() {
        assertDoubleEquals(6.2137119223733395, kiloMeterToStatuteMiles(10));
    }

    @Test
    public void testMetersPerSecondToKiloMetersPerHour() {
        assertDoubleEquals(3.6, msToKmh(1.0));
        assertDoubleEquals(20.0, kmhToMs(72.0));
    }

    @Test
    public void testDegrees2Nmea() {
        assertEquals(new ValueAndOrientation(4837.437396, North), latitude2nmea(48.6239566));
        assertEquals(new ValueAndOrientation(903.403596, East), longitude2nmea(9.0567266));
        assertEquals(new ValueAndOrientation(5047.5656, South), latitude2nmea(-50.79276));
        assertEquals(new ValueAndOrientation(927.1962, West), longitude2nmea(-9.45327));
    }

    @Test
    public void testNmea2Degrees() {
        assertDoubleEquals(48.6239566667, nmea2degrees(new ValueAndOrientation(4837.4374, East)));
        assertDoubleEquals(9.0567266667, nmea2degrees(new ValueAndOrientation(903.4036, North)));
        assertDoubleEquals(-50.7927616667, nmea2degrees(new ValueAndOrientation(5047.5657, West)));
        assertDoubleEquals(-9.45327, nmea2degrees(new ValueAndOrientation(927.1962, South)));
    }

    @Test
    public void testDegrees2Ddmm() {
        assertEquals("E 9\u00B0 3.343'", longitude2ddmm(9.0557233333));
        assertEquals("N 48\u00B0 37.437'", latitude2ddmm(48.6239566667));
    }

    @Test
    public void testDdmm2Longitude() {
        assertEquals(1.0, ddmm2longitude("E1\u00B00'"));
        assertEquals(10.1666667, ddmm2longitude("  E 0010  \u00B0  0010  \"  "));
        assertEquals(1.0, ddmm2longitude("E 1\u00B0 0'"));
        assertEquals(-1.0, ddmm2longitude("W 1\u00B0 0'"));
        assertEquals(9.0557233, ddmm2longitude("E 9\u00B0 3.3434'"));
        assertEquals(9.0557233, ddmm2longitude("E 9\u00B0 3.3434'"));
        assertEquals(9.0557233, ddmm2longitude("E 9\u00B0 3.3434\u2018"));
        assertEquals(9.0557233, ddmm2longitude("E 9\u00B0 3,3434\u2018"));
        assertNull(ddmm2longitude("E 9.1\u00B0 3.3434'"));
        assertNull(ddmm2longitude("E 9\u00B0 3.3434"));
    }

    @Test
    public void testMsfs20202Longitude() {
        assertEquals(14.26, ddmmss2longitude("E14\u00B0 15' 36.00\""));
        // Expected :E14° 15' 36.00"
        // Actual   :E 14° 15' 36.000"
        assertEquals("E 14\u00B0 15' 36.000\"", longitude2ddmmss(14.26));
    }

    @Test
    public void testDdmm2Latitude() {
        assertEquals(1.0, ddmm2latitude("N1\u00B00'"));
        assertEquals(10.0, ddmm2latitude(" N 010 \u00B0 000 \" "));
        assertEquals(-1.0, ddmm2latitude("S 1\u00B0 0'"));
        assertEquals(48.6239566, ddmm2latitude("N 48\u00B0 37.437395'"));
        assertEquals(48.6239566, ddmm2latitude("N 48\u00B0 37.437395\u2018"));
        assertEquals(48.6239566, ddmm2latitude("N 48\u00B0 37,437395\u2018"));
    }

    @Test
    public void testDegrees2Ddmmss() {
        assertEquals("E 9\u00B0 3' 20.604\"", longitude2ddmmss(9.0557233333));
        assertEquals("W 9\u00B0 3' 20.604\"", longitude2ddmmss(-9.0557233333));
        assertEquals("N 48\u00B0 37' 26.244\"", latitude2ddmmss(48.6239566667));
        assertEquals("N 0\u00B0 0' 0.000\"", latitude2ddmmss(0.0));
    }

    @Test
    public void testDdmmss2Longitude() {
        assertEquals(0.0, ddmmss2longitude("  E  0  \u00B0  0  '  0\"  "));
        assertEquals(1.0, ddmmss2longitude("E 1\u00B0 0' 0\""));
        assertEquals(-1.0, ddmmss2longitude("W 1\u00B0 0' 0\""));
        assertEquals(9.0557233, ddmmss2longitude("E 9\u00B0 3' 20.604\""));
        assertEquals(9.0557233, ddmmss2longitude("E 9\u00B0 3\u2018 20.604\""));
        assertEquals(9.0557233, ddmmss2longitude("E 9\u00B0 3' 20.604\""));
        assertNull(ddmmss2longitude("E 9.1\u00B0 3' 20.604\""));
        assertNull(ddmmss2longitude("E 9\u00B0 3.1' 20.604\""));
    }

    @Test
    public void testDdmmss2Latitude() {
        assertEquals(0.0, ddmmss2latitude("  E  0  \u00B0  0  '  0\"  "));
        assertEquals(1.0, ddmmss2latitude("N 1\u00B0 0' 0\""));
        assertEquals(-1.0, ddmmss2latitude("S 1\u00B0 0' 0\""));
        assertEquals(48.6239566, ddmmss2latitude("N 48\u00B0 37' 26.2438\""));
        assertEquals(48.6239566, ddmmss2latitude("N 48\u00B0 37\u2018 26.2438\""));
        assertEquals(48.6239566, ddmmss2latitude("N 48\u00B0 37\u2018 26,2438\""));
    }
}
