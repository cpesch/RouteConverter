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

import slash.common.io.Transfer;

import java.math.BigDecimal;
import java.util.prefs.Preferences;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static slash.common.io.Transfer.ceilFraction;
import static slash.common.io.Transfer.roundFraction;
import static slash.navigation.common.UnitConversion.feetToMeters;
import static slash.navigation.common.UnitConversion.meterToFeets;

/**
 * Provides navigation conversion functionality.
 *
 * @author Christian Pesch
 */

public class NavigationConversion {
    private static final String POSITION_MAXIMUM_FRACTION_DIGITS = "positionMaximumFractionDigits";
    private static final String ELEVATION_MAXIMUM_FRACTION_DIGITS = "elevationMaximumFractionDigits";
    private static final String HEADING_MAXIMUM_FRACTION_DIGITS = "headingMaximumFractionDigits";
    private static final String SPEED_MAXIMUM_FRACTION_DIGITS = "speedMaximumFractionDigits";
    private static final String TEMPERATURE_MAXIMUM_FRACTION_DIGITS = "temperatureMaximumFractionDigits";
    private static final String ACCURACY_MAXIMUM_FRACTION_DIGITS = "accuracyMaximumFractionDigits";

    private NavigationConversion() {}

    private static final Preferences preferences = Preferences.userNodeForPackage(NavigationConversion.class);

    /* 6371014 would be a better value, but this seems to be used by
       Map&Guide Tourenplaner when exporting to XML. */
    private static final double EARTH_RADIUS = 6371000.0;

    private static final double ALTITUDE_146m = 210945416903L;
    private static final double ELEVATION_146m = 146;
    private static final double ALTITUDE_6m = 210945415755L;
    private static final double ELEVATION_6m = 6;


    private static double roundWgs84(double wgs84) {
        return floor(wgs84 * 100000.0) / 100000.0;
    }

    private static long roundMercator(double wgs84, double mercator) {
        if (wgs84 > 0.0)
            return round(ceil(mercator));
        else
            return round(floor(mercator));
    }

    // see http://en.wikipedia.org/wiki/Mercator_projection

    public static double mercatorXToWgs84Longitude(long x) {
        double longitude = x * 180.0 / (EARTH_RADIUS * PI);
        return roundWgs84(longitude);
    }

    public static double mercatorYToWgs84Latitude(long y) {
        double latitude = 2.0 * (atan(exp(y / EARTH_RADIUS)) - PI / 4.0) / PI * 180.0;
        return roundWgs84(latitude);
    }

    public static long wgs84LongitudeToMercatorX(double longitude) {
        double x = longitude * EARTH_RADIUS * PI / 180.0;
        return roundMercator(longitude, x);
    }

    public static long wgs84LatitudeToMercatorY(double latitude) {
        double y = log(tan(latitude * PI / 360.0 + PI / 4.0)) * EARTH_RADIUS;
        return roundMercator(latitude, y);
    }

    // see http://de.wikipedia.org/wiki/Gau%C3%9F-Kr%C3%BCger-Koordinatensystem

    private static final double aBessel = 6377397.155;
    private static final double bBessel = 6356078.962;
    private static final double e2Bessel = (pow(aBessel, 2) - pow(bBessel, 2)) / pow(aBessel, 2);
    private static final double nBessel = (aBessel - bBessel) / (aBessel + bBessel);

    private static final double aWgs84 = 6378137;
    private static final double bWgs84 = 6356752.314;
    private static final double e2Wgs84 = (pow(aWgs84, 2) - pow(bWgs84, 2)) / pow(aWgs84, 2);

    private static final double alphaGk2Wgs84 = (aBessel + bBessel) / 2 * (1 + pow(nBessel, 2) / 4 + pow(nBessel, 4) / 64);
    private static final double betaGk2Wgs84 = nBessel * 3 / 2 - pow(nBessel, 3) * 27 / 32 + pow(nBessel, 5) * 269 / 512;
    private static final double gammaGk2Wgs84 = pow(nBessel, 2) * 21 / 16 - pow(nBessel, 4) * 55 / 32;
    private static final double deltaGk2Wgs84 = pow(nBessel, 3) * 151 / 96 - pow(nBessel, 5) * 417 / 128;
    private static final double epsilonGk2Wgs84 = pow(nBessel, 4) * 1097 / 512;

    public static double[] gaussKruegerRightHeightToWgs84LongitudeLatitude(double right, double height) {
        /* from http://www.wolfgang-back.com/navigauss.php
        double rho = 180.0 / Math.PI;
        double e2 = 0.0067192188;
        double c = 6398786.849;
        double sy = 3.0;
        int mKen = new Double(right / 1000000).intValue();
        double rm = right - mKen * 1000000 - 500000;
        double bI = height / 10000855.7646;
        double bII = bI * bI;
        double bf = 325632.08677 * bI * ((((((0.00000562025 * bII + 0.00022976983) * bII - 0.00113566119) * bII + 0.00424914906) * bII - 0.00831729565) * bII + 1));
        bf /= 3600.0 * rho;
        double co = Math.cos(bf);
        double g2 = e2 * (co * co);
        double g1 = c / Math.sqrt(1 + g2);
        double t = Math.tan(bf);
        double fa = rm / g1;
        double latitude = bf - fa * fa * t * (1 + g2) / 2
                + fa * fa * fa * fa * t * (5 + 3 * t * t + 6 * g2 - 6 * g2 * t * t) / 24;
        latitude = latitude * rho;
        double dl = fa
                - fa * fa * fa * (1 + 2 * t * t + g2) / 6
                + fa * fa * fa * fa * fa * (1 + 28 * t * t + 24 * t * t * t * t) / 120;
        double longitude = dl * rho / co + mKen * 3;
        return new double[]{longitude, latitude};
        */

        // from http://www.geoclub.de/ftopic8332.html
        double h = 4.21;

        // Umrechnung GK nach B, L
        int y0 = (int) (right / 1000000);
        double L0 = y0 * 3;
        int yInt = (int) (right - y0 * 1000000 - 500000);
        double B0 = height / alphaGk2Wgs84;
        double Bf = (B0 + betaGk2Wgs84 * sin(2 * B0) + gammaGk2Wgs84 * sin(4 * B0) + deltaGk2Wgs84 * sin(6 * B0) + epsilonGk2Wgs84 * sin(8 * B0));
        double Nf = aBessel / sqrt(1 - e2Bessel * pow(sin(Bf), 2));
        double pif = sqrt(pow(aBessel, 2) / pow(bBessel, 2) * e2Bessel * pow(cos(Bf), 2));
        double tf = tan(Bf);
        double tf1 = tf / 2 / pow(Nf, 2) * (-1 - pow(pif, 2)) * pow(yInt, 2);
        double tf2 = tf / 24 / pow(Nf, 4) * (5 + 3 * pow(tf, 2) + 6 * pow(pif, 2) - 6 * pow(tf, 2) * pow(pif, 2) - 4 * pow(pif, 4) - 9 * pow(tf, 2) * pow(pif, 4)) * pow(yInt, 4);
        // double tf3 = tf / 720 / Math.pow(Nf, 6) * (-61 - 90 * Math.pow(tf, 2) - 45 * Math.pow(tf, 4) - 107 * Math.pow(pif, 2) + 162 * Math.pow(tf, 2) * Math.pow(pif, 2) + 45 * Math.pow(tf, 4) * Math.pow(pif, 2)) * Math.pow(yInt, 6);
        // double tf4 = tf / 40320 / Math.pow(Nf, 8) * (1385 + 3663 * Math.pow(tf, 2) + 4095 * Math.pow(tf, 4) + 1575 * Math.pow(tf, 6)) * Math.pow(yInt, 8);
        double B = (Bf + tf1 + tf2) * 180 / PI;
        double l1 = 1 / Nf / cos(Bf) * yInt;
        double l2 = (1 / pow(Nf, 3) / 6 / cos(Bf)) * (-1 - 2 * pow(tf, 2) - pow(pif, 2)) * pow(yInt, 3);
        // double l3 = 1 / Math.pow(Nf, 5) / 120 / Math.cos(Bf) * (5 + 28 * Math.pow(tf, 2) + 24 * Math.pow(tf, 4) + 6 * Math.pow(pif, 2) + 8 * Math.pow(tf, 2) * Math.pow(pif, 2)) * Math.pow(yInt, 5);
        // double l4 = 1 / Math.pow(Nf, 7) / 15040 / Math.cos(Bf) * (-61 - 622 * Math.pow(tf, 2) - 1320 * Math.pow(tf, 4) - 720 * Math.pow(tf, 6)) * Math.pow(yInt, 7);
        double L = L0 + (l1 + l2) * 180 / PI;

        // Ell. Koordinaten auf dem Bessel-Ellipsoid
        double N = aBessel / sqrt(1 - e2Bessel * pow(sin(B / 180 * PI), 2));
        double x1 = (N + h) * cos(B / 180 * PI) * cos(L / 180 * PI);
        double y1 = (N + h) * cos(B / 180 * PI) * sin(L / 180 * PI);
        double z1 = (N * pow(bBessel, 2) / pow(aBessel, 2) + h) * sin(B / 180 * PI);

        // Rotierte Vektoren
        double x2 = x1 * 1 + y1 * 0.0000119021759 + z1 * 0.000000218166156;
        double y2 = x1 * -0.0000119021759 + y1 * 1 + z1 * -0.000000979323636;
        double z2 = x1 * -0.000000218166156 + y1 * 0.0000009793236 + z1 * 1;

        // Translationen anbringen
        double x = x2 * 0.9999933 + (598.095);
        double y = y2 * 0.9999933 + (73.707);
        double z = z2 * 0.9999933 + (418.197);

        // Vektoren (in ETRF89)
        double s = sqrt(pow(x, 2) + pow(y, 2));
        double T = atan(z * aWgs84 / (s * bWgs84));
        double B2 = atan((z + e2Wgs84 * pow(aWgs84, 2) / bWgs84 * pow(sin(T), 3)) / (s - e2Wgs84 * aWgs84 * pow(cos(T), 3)));
        double L2 = atan(y / x);
        // double N2 = aWgs84 / Math.sqrt(1 - e2Wgs84 * Math.pow(Math.sin(B2), 2));
        // h = s / Math.cos(B2) - N2;
        double latitude = B2 * 180 / PI;
        double longitude = L2 * 180 / PI;
        return new double[]{longitude, latitude};
    }

    private static final double alphaWgs842Gk = (aBessel + bBessel) / 2 * (1 + pow(nBessel, 2) / 4 + pow(nBessel, 4) / 64);
    private static final double betaWgs842Gk = -3 * nBessel / 2 + 9 * pow(nBessel, 3) / 16 - 3 * pow(nBessel, 5) / 32;
    private static final double gammaWgs842Gk = 15 * pow(nBessel, 2) / 16 - 15 * pow(nBessel, 4) / 32;
    private static final double deltaWgs842Gk = -35 * pow(nBessel, 3) / 48 + 105 * pow(nBessel, 5) / 256;
    private static final double epsilonWgs842Gk = 315 * pow(nBessel, 4) / 512;

    public static double[] wgs84LongitudeLatitudeToGaussKruegerRightHeight(double longitude, double latitude) {
        /* from http://www.wolfgang-back.com/navigauss.php
        double rho = 180.0 / Math.PI;
        double e2 = 0.0067192188;
        double c = 6398786.849;
        double sy = 3.0;
        double bf = latitude / rho;
        double g = 111120.61962 * latitude
                - 15988.63853 * Math.sin(2 * bf)
                + 16.72995 * Math.sin(4 * bf)
                - 0.02178 * Math.sin(6 * bf)
                + 0.00003 * Math.sin(8 * bf);
        double co = Math.cos(bf);
        double g2 = e2 * (co * co);
        double g1 = c / Math.sqrt(1 + g2);
        double t = Math.sin(bf) / Math.cos(bf);
        double dl = longitude - sy * 3;
        double fa = co * dl / rho;
        double height = g
                + fa * fa * t * g1 / 2
                + fa * fa * fa * fa * t * g1 * (5 - t * t + 9 * g2) / 24;
        double rm = fa * g1
                + fa * fa * fa * g1 * (1 - t * t + g2) / 6
                + fa * fa * fa * fa * fa * g1 * (5 - 18 * t * t * t * t * t * t) / 120;
        double right = rm + sy * 1000000 + 500000;
        return new double[]{right, height};
        */

        // from http://www.geoclub.de/ftopic8332.html
        double h = 4.21;

        // Ell. Koordinaten auf dem WGS-Ellipsoid
        double nWgs84 = aWgs84 / sqrt(1 - e2Wgs84 * pow(sin(latitude / 180 * PI), 2));
        double x1 = (nWgs84 + h) * cos(latitude / 180 * PI) * cos(longitude / 180 * PI);
        double y1 = (nWgs84 + h) * cos(latitude / 180 * PI) * sin(longitude / 180 * PI);
        double z1 = (nWgs84 * pow(bWgs84, 2) / pow(aWgs84, 2) + h) * sin(latitude / 180 * PI);

        // Rotierte Vektoren
        double x2 = x1 * 1 + y1 * -0.0000119021759 + z1 * -0.000000218166156;
        double y2 = x1 * 0.0000119021759 + y1 * 1 + z1 * 0.000000979323636;
        double z2 = x1 * 0.000000218166156 + y1 * -0.0000009793236 + z1 * 1;

        // Translationen anbringen
        double x = x2 * 0.9999933 + (-598.095);
        double y = y2 * 0.9999933 + (-73.707);
        double z = z2 * 0.9999933 + (-418.197);

        // Vektoren (in ETRF89)
        double s = sqrt(pow(x, 2) + pow(y, 2));
        double T = atan(z * aBessel / (s * bBessel));
        double B = atan((z + e2Bessel * pow(aBessel, 2) / bBessel * pow(sin(T), 3)) / (s - e2Bessel * aBessel * pow(cos(T), 3)));
        double L = atan(y / x);
        double N = aBessel / sqrt(1 - e2Bessel * pow(sin(B), 2));
        // h = s / Math.cos(B) - N;
        double B1 = B * 180 / PI;
        double L1 = L * 180 / PI;

        // Umrechnung B,L in GK
        int L0;
        if (Math.abs(L1 - 6) < 1.5)
            L0 = 6;
        else if (Math.abs(L1 - 9) < 1.5)
            L0 = 9;
        else if (Math.abs(L1 - 12) < 1.5)
            L0 = 12;
        else
            L0 = 15;
        double I = (L1 - L0) * PI / 180;
        double B3 = B1 / 180 * PI;
        double pi = sqrt(pow(aBessel, 2) / pow(bBessel, 2) * e2Bessel * pow(cos(B3), 2));
        double t2 = tan(B3);
        double Bogenlaenge = alphaWgs842Gk * (B3 + betaWgs842Gk * sin(2 * B3) + gammaWgs842Gk * sin(4 * B3) + deltaWgs842Gk * sin(6 * B3) + epsilonWgs842Gk * sin(8 * B3));
        double BL1 = t2 / 2 * nWgs84 * pow(cos(B3), 2) * pow(I, 2);
        double BL2 = t2 / 24 * nWgs84 * pow(cos(B3), 4) * (5 - pow(t2, 2) + 9 * pow(pi, 2) + 4 * pow(pi, 4)) * pow(I, 4);
        // double BL3 = t2 / 720 * nWgs84 * Math.pow(Math.cos(B3), 6) * (61 - 58 * Math.pow(t2, 2) - 330 * t2 * Math.pow(pi, 2)) * Math.pow(I, 6);
        // double BL4 = t2 / 40320 * nWgs84 * Math.pow(Math.cos(B3), 8) * (1385 - 3111 * Math.pow(t2, 2) + 543 * Math.pow(t2, 4) - Math.pow(t2, 6)) * Math.pow(I, 8);
        double height = Bogenlaenge + BL1 + BL2;
        double RW1 = N * cos(B3) * I;
        double RW2 = N / 6 * pow(cos(B3), 3) * (1 - pow(t2, 2) + pow(pi, 2)) * pow(I, 3);
        // double RW3 = N / 120 * Math.pow(Math.cos(B3), 5) * (5 - 18 * Math.pow(t2, 2) + Math.pow(t2, 4) + 14 * Math.pow(pi, 2) - 58 * Math.pow(t2, 2) * Math.pow(pi, 2)) * Math.pow(I, 5);
        // double RW4 = N / 5040 * Math.pow(Math.cos(B3), 7) * (61 - 479 * Math.pow(t2, 2) + 179 * Math.pow(t2, 4)) * Math.pow(I, 7);
        double right = RW1 + RW2 + 500000 + L0 / 3 * 1000000;
        return new double[]{right, height};
    }

    public static double bcrAltitudeToElevationMeters(long altitude) {
        double feet = (altitude - ALTITUDE_6m) *
                (meterToFeets(ELEVATION_146m - ELEVATION_6m) / (ALTITUDE_146m - ALTITUDE_6m));
        double meters = feetToMeters(feet) + ELEVATION_6m;
        return ceilFraction(meters, 2);
    }

    public static long elevationMetersToBcrAltitude(double elevation) {
        double feet = meterToFeets(elevation - ELEVATION_6m);
        double altitude = (feet) * ((ALTITUDE_146m - ALTITUDE_6m) / meterToFeets(ELEVATION_146m - ELEVATION_6m));
        altitude += ALTITUDE_6m;
        return (long) floor(altitude);
    }

    private static boolean isReduceDecimalPlaceToReasonablePrecision() {
        return preferences.getBoolean("reduceDecimalPlacesToReasonablePrecision", true);
    }

    public static Double formatDouble(Double aDouble, int maximumFractionCount) {
        if (aDouble == null)
            return null;
        if (isReduceDecimalPlaceToReasonablePrecision())
            aDouble = roundFraction(aDouble, maximumFractionCount);
        return aDouble;
    }

    public static BigDecimal formatBigDecimal(Double aDouble, int maximumFractionCount) {
        if (aDouble == null)
            return null;
        if (isReduceDecimalPlaceToReasonablePrecision())
            aDouble = roundFraction(aDouble, maximumFractionCount);
        return BigDecimal.valueOf(aDouble);
    }

    private static String formatDoubleAsString(Double aDouble, int maximumFractionCount) {
        if (aDouble != null && isReduceDecimalPlaceToReasonablePrecision())
            aDouble = roundFraction(aDouble, maximumFractionCount);
        return Transfer.formatDoubleAsString(aDouble);
    }

    public static String formatPositionAsString(Double longitudeOrLatitude) {
        int maximumFractionDigits = preferences.getInt(POSITION_MAXIMUM_FRACTION_DIGITS, 7);
        return formatDoubleAsString(longitudeOrLatitude, maximumFractionDigits);
    }

    public static String formatElevationAsString(Double elevation) {
        int maximumFractionDigits = preferences.getInt(ELEVATION_MAXIMUM_FRACTION_DIGITS, 1);
        return formatDoubleAsString(elevation, maximumFractionDigits);
    }

    public static String formatAccuracyAsString(Double accuracy) {
        int maximumFractionDigits = preferences.getInt(ACCURACY_MAXIMUM_FRACTION_DIGITS, 6);
        return formatDoubleAsString(accuracy, maximumFractionDigits);
    }

    public static String formatHeadingAsString(Double heading) {
        int maximumFractionDigits = preferences.getInt(HEADING_MAXIMUM_FRACTION_DIGITS, 1);
        return formatDoubleAsString(heading, maximumFractionDigits);
    }

    public static String formatSpeedAsString(Double speed) {
        int maximumFractionDigits = preferences.getInt(SPEED_MAXIMUM_FRACTION_DIGITS, 1);
        return formatDoubleAsString(speed, maximumFractionDigits);
    }

    public static String formatTemperatureAsString(Double temperature) {
        int maximumFractionDigits = preferences.getInt(TEMPERATURE_MAXIMUM_FRACTION_DIGITS, 1);
        return formatDoubleAsString(temperature, maximumFractionDigits);
    }

    public static BigDecimal formatPosition(Double longitudeOrLatitude) {
        int maximumFractionDigits = preferences.getInt(POSITION_MAXIMUM_FRACTION_DIGITS, 7);
        return formatBigDecimal(longitudeOrLatitude, maximumFractionDigits);
    }

    public static BigDecimal formatElevation(Double elevation) {
        int maximumFractionDigits = preferences.getInt(ELEVATION_MAXIMUM_FRACTION_DIGITS, 1);
        return formatBigDecimal(elevation, maximumFractionDigits);
    }

    public static BigDecimal formatAccuracy(Double accuracy) {
        int maximumFractionDigits = preferences.getInt(ACCURACY_MAXIMUM_FRACTION_DIGITS, 6);
        return formatBigDecimal(accuracy, maximumFractionDigits);
    }

    public static BigDecimal formatHeading(Double heading) {
        int maximumFractionDigits = preferences.getInt(HEADING_MAXIMUM_FRACTION_DIGITS, 1);
        return formatBigDecimal(heading, maximumFractionDigits);
    }

    public static BigDecimal formatSpeed(Double speed) {
        int maximumFractionDigits = preferences.getInt(SPEED_MAXIMUM_FRACTION_DIGITS, 1);
        return formatBigDecimal(speed, maximumFractionDigits);
    }

    public static Double formatSpeedAsDouble(Double speed) {
        int maximumFractionDigits = preferences.getInt(SPEED_MAXIMUM_FRACTION_DIGITS, 1);
        return formatDouble(speed, maximumFractionDigits);
    }

    public static Double formatTemperatureAsDouble(Double temperature) {
        int maximumFractionDigits = preferences.getInt(TEMPERATURE_MAXIMUM_FRACTION_DIGITS, 1);
        return formatDouble(temperature, maximumFractionDigits);
    }
}
