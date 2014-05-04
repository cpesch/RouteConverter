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

import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.io.Transfer.roundMeterToMillimeterPrecision;

public class BearingTest {

    /*
    * Calculate geodesic distance (in m) between two points specified by latitude/longitude (in numeric degrees)
    * using Vincenty inverse formula for ellipsoids.
    *
    * From http://www.movable-type.co.uk/scripts/latlong-vincenty.html
    */
    private static double vincentyDistance(double lon1, double lat1, double lon2, double lat2) {
        double a = 6378137.0, b = 6356752.3142, f = 1 / 298.257223563;  // WGS-84 ellipsiod
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);

        double lambda = L, lambdaP = 2 * Math.PI, sinSigma = 0.0, cosSigma = 0.0,
                sigma = 0.0, cosSqAlpha = 0.0, cos2SigmaM = 0.0;
        int iterLimit = 20;
        while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0) {
            double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt((cosU2 * sinLambda) * (cosU2 * sinLambda) +
                    (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda) * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda));
            if (sinSigma == 0.0)
                return 0.0;  // co-incident points
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            if (Double.isNaN(cos2SigmaM))
                cos2SigmaM = 0.0;  // equatorial line: cosSqAlpha=0
            double C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha));
            lambdaP = lambda;
            lambda = L + (1.0 - C) * f * sinAlpha *
                    (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)));
        }
        if (iterLimit == 0)
            return Double.NaN;

        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)));
        double B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)));
        double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
                B / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)));
        double s = b * A * (sigma - deltaSigma);
        s = roundMeterToMillimeterPrecision(s); // round to 1mm precision
        return s;
    }

    @Test
    public void testVincentyDistance() {
        assertDoubleEquals(0.0, vincentyDistance(10.03200, 53.56948, 10.03200, 53.56948));
        assertDoubleEquals(0.1295, vincentyDistance(10.03200, 53.56948, 10.032001, 53.569481));
        assertDoubleEquals(1.2952, vincentyDistance(10.03200, 53.56948, 10.03201, 53.56949));
        assertDoubleEquals(12.9522, vincentyDistance(10.0320, 53.5694, 10.0321, 53.5695));
        assertDoubleEquals(144472.5478, vincentyDistance(01.5040, 53.0902, 00.0833, 52.1219));
    }

    @Test
    public void testIdenticalPositionBearing() {
        Bearing bearing = Bearing.calculateBearing(10.03200, 53.56948, 10.03200, 53.56948);
        assertDoubleEquals(0.0, bearing.getAngle());
        assertDoubleEquals(0.0, bearing.getBackAzimuth());
        assertDoubleEquals(0.0, bearing.getDistance());
    }

    @Test
    public void testModifiedLongitudes6thPositionBehindTheCommaBearing() {
        Bearing bearing = Bearing.calculateBearing(10.03200, 53.56948, 10.032001, 53.56948);
        assertDoubleEquals(90.0, bearing.getAngle());
        assertDoubleEquals(270.0000006116776, bearing.getBackAzimuth()); // should be 270.0 ?
        assertDoubleEquals(0.0662, bearing.getDistance());
    }

    @Test
    public void testModifiedLatitudes6thPositionBehindTheCommaBearing() {
        Bearing bearing = Bearing.calculateBearing(10.03200, 53.56948, 10.03200, 53.569481);
        assertDoubleEquals(0.0, bearing.getAngle());
        assertDoubleEquals(180.0, bearing.getBackAzimuth());
        assertDoubleEquals(0.1112, bearing.getDistance());
    }

    @Test
    public void testBearingAgainstVincentyDistance() {
        Bearing bearing = Bearing.calculateBearing(01.5040, 53.0902, 00.0833, 52.1219);
        assertDoubleEquals(222.33867425245487, bearing.getAngle());
        assertDoubleEquals(41.20989654768979, bearing.getBackAzimuth());
        assertDoubleEquals(144472.5478, bearing.getDistance());
        assertDoubleEquals(vincentyDistance(01.5040, 53.0902, 00.0833, 52.1219), bearing.getDistance());
    }
}
