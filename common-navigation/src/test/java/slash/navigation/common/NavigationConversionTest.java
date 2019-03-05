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

package slash.navigation.common;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.assertNearBy;
import static slash.navigation.common.NavigationConversion.*;

public class NavigationConversionTest {
    @Test
    public void testMercatorToWgs84() {
        assertDoubleEquals(10.03200, mercatorXToWgs84Longitude(1115508));
        assertDoubleEquals(53.56948, mercatorYToWgs84Latitude(7081108));

        assertDoubleEquals(9.45327, mercatorXToWgs84Longitude(1051156));
        assertDoubleEquals(50.79276, mercatorYToWgs84Latitude(6577349));
    }

    @Test
    public void testWgs84ToMercator() {
        assertEquals(1115508, wgs84LongitudeToMercatorX(10.03200));
        // NH-TopTrans makes 7081108 from this - don't know why
        assertEquals(7081107, wgs84LatitudeToMercatorY(53.56948));

        assertEquals(1051156, wgs84LongitudeToMercatorX(9.45327));
        assertEquals(6577349, wgs84LatitudeToMercatorY(50.79276));
    }

    @Test
    public void testGaussKruegerRightToWgs84LongitudeSouthWest() {
        assertNearBy(13.35573, gaussKruegerRightHeightToWgs84LongitudeLatitude(4592172, 5819212)[0]);
    }

    @Test
    public void testGaussKruegerHeightToWgs84LatitudeSouthWest() {
        assertNearBy(52.49830, gaussKruegerRightHeightToWgs84LongitudeLatitude(4592172, 5819212)[1]);
    }

    @Test
    public void testWgs84LongitudeToGaussKruegerRightSouthWest() {
        assertNearBy(4592172, wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.35573, 52.49830)[0]);
    }

    @Test
    public void testWgs84LatitudeToGaussKruegerHeightSouthWest() {
        assertNearBy(5819212, wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.35573, 52.49830)[1]);
    }

    @Test
    public void testGaussKruegerRightToWgs84LongitudeNorthEast() {
        assertNearBy(13.53667, gaussKruegerRightHeightToWgs84LongitudeLatitude(5400904, 5826585)[0]);
    }

    @Test
    public void testGaussKruegerHeightToWgs84LatitudeNorthEast() {
        assertNearBy(52.56332, gaussKruegerRightHeightToWgs84LongitudeLatitude(5400904, 5826585)[1]);
    }

    @Test
    public void testWgs84LongitudeToGaussKruegerRightNorthEast() {
        assertNearBy(5400904, wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.53667, 52.56332)[0]);
    }

    @Test
    public void testWgs84LatitudeToGaussKruegerHeightNorthEast() {
        assertNearBy(5826585, wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.53667, 52.56332)[1]);
    }

    @Test
    public void testSemiCircleToDegree() {
        assertDoubleEquals(11.346305720508099, semiCircleToDegree(135366700));
        assertDoubleEquals(-11.346305720508099, semiCircleToDegree(-135366700));
    }

    @Test
    public void testDegreeToSemiCircle() {
        assertDoubleEquals(135366700, degreeToSemiCircle(11.346305720508099));
        assertDoubleEquals(-135366700, degreeToSemiCircle(-11.346305720508099));
    }

    @Test
    public void testBcrToElevation() {
        assertDoubleEquals(-0.09, bcrAltitudeToElevationMeters(210945415705L));
        assertDoubleEquals(6.0, bcrAltitudeToElevationMeters(210945415755L));
        assertDoubleEquals(146.0, bcrAltitudeToElevationMeters(210945416903L));
    }

    @Test
    public void testElevationToBcr() {
        assertEquals(210945415755L, elevationMetersToBcrAltitude(6.0));
        assertEquals(210945416903L, elevationMetersToBcrAltitude(146.0));
        assertEquals(210945415705L, elevationMetersToBcrAltitude(0.0));
    }

    @Test
    public void testFormatDoubleAsBigDecimal() {
        assertEquals(new BigDecimal("1.0"), formatBigDecimal(1.0, 5));
        assertEquals(new BigDecimal("1.5"), formatBigDecimal(1.5, 5));
        assertEquals(new BigDecimal("1.05"), formatBigDecimal(1.05, 5));
        assertEquals(new BigDecimal("1.005"), formatBigDecimal(1.005, 5));
        assertEquals(new BigDecimal("1.00004"), formatBigDecimal(1.00004, 5));
        assertEquals(new BigDecimal("1.00044"), formatBigDecimal(1.00044, 5));
        assertEquals(new BigDecimal("1.00045"), formatBigDecimal(1.00045, 5));
        assertEquals(new BigDecimal("1.00005"), formatBigDecimal(1.00005, 5));
        assertEquals(new BigDecimal("1.000004"), formatBigDecimal(1.000004, 6));
        assertEquals(new BigDecimal("1.000005"), formatBigDecimal(1.000005, 6));
    }
}
