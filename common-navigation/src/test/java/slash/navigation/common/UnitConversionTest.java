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

import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.common.UnitConversion.ddmm2degrees;
import static slash.navigation.common.UnitConversion.degrees2ddmm;
import static slash.navigation.common.UnitConversion.feetToMeters;
import static slash.navigation.common.UnitConversion.kilometerToStatuteMiles;
import static slash.navigation.common.UnitConversion.kmhToMs;
import static slash.navigation.common.UnitConversion.msToKmh;

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
    public void testDegrees2Ddmm() {
        assertDoubleEquals(4837.4374, degrees2ddmm(48.6239566));
        assertDoubleEquals(903.4036, degrees2ddmm(9.0567266));

        assertDoubleEquals(5047.5657, degrees2ddmm(50.79276));
        assertDoubleEquals(927.1962, degrees2ddmm(9.45327));
    }

    @Test
    public void testDdmm2Degrees() {
        assertDoubleEquals(48.6239566, ddmm2degrees(4837.4374));
        assertDoubleEquals(9.0557233, ddmm2degrees(903.3434));
    }
}
