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

package slash.navigation.util;

import slash.common.TestCase;

public class ConversionTest extends TestCase {
    public void testFeetToMeters() {
        assertEquals(4.572, Conversion.feetToMeters(15));
    }

    public void testMetersPerSecondToKilometersPerHour() {
        assertEquals(3.6, Conversion.msToKmh(1.0));
        assertEquals(20.0, Conversion.kmhToMs(72.0));
    }

    public void testMercatorToWgs84() {
        assertEquals(10.03200, Conversion.mercatorXToWgs84Longitude(1115508));
        assertEquals(53.56948, Conversion.mercatorYToWgs84Latitude(7081108));

        assertEquals(9.45327, Conversion.mercatorXToWgs84Longitude(1051156));
        assertEquals(50.79276, Conversion.mercatorYToWgs84Latitude(6577349));
    }

    public void testWgs84ToMercator() {
        assertEquals(1115508, Conversion.wgs84LongitudeToMercatorX(10.03200));
        // NH-TopTrans makes 7081108 from this - don't know why
        assertEquals(7081107, Conversion.wgs84LatitudeToMercatorY(53.56948));

        assertEquals(1051156, Conversion.wgs84LongitudeToMercatorX(9.45327));
        assertEquals(6577349, Conversion.wgs84LatitudeToMercatorY(50.79276));
    }


    public void testGaussKruegerRightToWgs84LongitudeSouthWest() {
        assertNearBy(13.35573, Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude(4592172, 5819212)[0]);
    }

    public void testGaussKruegerHeightToWgs84LatitudeSouthWest() {
        assertNearBy(52.49830, Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude(4592172, 5819212)[1]);
    }

    public void testWgs84LongitudeToGaussKruegerRightSouthWest() {
        assertNearBy(4592172, Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.35573, 52.49830)[0]);
    }

    public void testWgs84LatitudeToGaussKruegerHeightSouthWest() {
        assertNearBy(5819212, Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.35573, 52.49830)[1]);
    }


    public void testGaussKruegerRightToWgs84LongitudeNorthEast() {
        assertNearBy(13.53667, Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude(5400904, 5826585)[0]);
    }

    public void testGaussKruegerHeightToWgs84LatitudeNorthEast() {
        assertNearBy(52.56332, Conversion.gaussKruegerRightHeightToWgs84LongitudeLatitude(5400904, 5826585)[1]);
    }

    public void testWgs84LongitudeToGaussKruegerRightNorthEast() {
        assertNearBy(5400904, Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.53667, 52.56332)[0]);
    }

    public void testWgs84LatitudeToGaussKruegerHeightNorthEast() {
        assertNearBy(5826585, Conversion.wgs84LongitudeLatitudeToGaussKruegerRightHeight(13.53667, 52.56332)[1]);
    }


    public void testBcrToElevation() {
        assertEquals(-0.09, Conversion.bcrAltitudeToElevationMeters(210945415705L));
        assertEquals(6.0, Conversion.bcrAltitudeToElevationMeters(210945415755L));
        assertEquals(146.0, Conversion.bcrAltitudeToElevationMeters(210945416903L));
    }

    public void testElevationToBcr() {
        assertEquals(210945415755L, Conversion.elevationMetersToBcrAltitude(6.0));
        assertEquals(210945416903L, Conversion.elevationMetersToBcrAltitude(146.0));
        assertEquals(210945415705L, Conversion.elevationMetersToBcrAltitude(0.0));
    }

    public void testDegrees2Ddmm() {
        assertEquals(4837.4374, Conversion.degrees2ddmm(48.6239566));
        assertEquals(903.4036, Conversion.degrees2ddmm(9.0567266));

        assertEquals(5047.5657, Conversion.degrees2ddmm(50.79276));
        assertEquals(927.1962, Conversion.degrees2ddmm(9.45327));
    }

    public void testDdmm2Degrees() {
        assertEquals(48.6239566, Conversion.ddmm2degrees(4837.4374));
        assertEquals(9.0557233, Conversion.ddmm2degrees(903.3434));
    }
}
