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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Provides conversion functionality.
 *
 * @author Christian Pesch
 */

public class Conversion {

    /* 6371014 would be a better value, but this seems to be used by
      Map&Guide Tourenplaner when exporting to XML. */
    static final double EARTH_RADIUS = 6371000.0;

    private static final double METER_OF_A_FEET = 0.3048;
    private static final double KILOMETER_OF_A_KNOT = 1.8520043;
    private static final double ALTITUDE_146m = 210945416903L;
    private static final double ELEVATION_146m = 146;
    private static final double ALTITUDE_6m = 210945415755L;
    private static final double ELEVATION_6m = 6;


    private static double roundWgs84(double wgs84) {
        return Math.floor(wgs84 * 100000.0) / 100000.0;
    }

    // see http://en.wikipedia.org/wiki/Mercator_projection

    public static double mercatorXToWgs84Longitude(long x) {
        double longitude = x * 180.0 / (EARTH_RADIUS * Math.PI);
        return roundWgs84(longitude);
    }

    public static double mercatorYToWgs84Latitude(long y) {
        double latitude = 2.0 * (Math.atan(Math.exp(y / EARTH_RADIUS)) - Math.PI / 4.0) / Math.PI * 180.0;
        return roundWgs84(latitude);
    }


    private static long roundMercator(double wgs84, double mercator) {
        if (wgs84 > 0.0)
            return Math.round(Math.ceil(mercator));
        else
            return Math.round(Math.floor(mercator));
    }

    public static long wgs84LongitudeToMercatorX(double longitude) {
        double x = longitude * EARTH_RADIUS * Math.PI / 180.0;
        return roundMercator(longitude, x);
    }

    public static long wgs84LatitudeToMercatorY(double latitude) {
        double y = Math.log(Math.tan(latitude * Math.PI / 360.0 + Math.PI / 4.0)) * EARTH_RADIUS;
        return roundMercator(latitude, y);
    }


    private static final double aBessel = 6377397.155;
    private static final double bBessel = 6356078.962;
    private static final double e2Bessel = (Math.pow(aBessel, 2) - Math.pow(bBessel, 2)) / Math.pow(aBessel, 2);
    private static final double nBessel = (aBessel - bBessel) / (aBessel + bBessel);

    private static final double aWgs84 = 6378137;
    private static final double bWgs84 = 6356752.314;
    private static final double e2Wgs84 = (Math.pow(aWgs84, 2) - Math.pow(bWgs84, 2)) / Math.pow(aWgs84, 2);

    private static final double alphaGk2Wgs84 = (aBessel + bBessel) / 2 * (1 + Math.pow(nBessel, 2) / 4 + Math.pow(nBessel, 4) / 64);
    private static final double betaGk2Wgs84 = nBessel * 3 / 2 - Math.pow(nBessel, 3) * 27 / 32 + Math.pow(nBessel, 5) * 269 / 512;
    private static final double gammaGk2Wgs84 = Math.pow(nBessel, 2) * 21 / 16 - Math.pow(nBessel, 4) * 55 / 32;
    private static final double deltaGk2Wgs84 = Math.pow(nBessel, 3) * 151 / 96 - Math.pow(nBessel, 5) * 417 / 128;
    private static final double epsilonGk2Wgs84 = Math.pow(nBessel, 4) * 1097 / 512;

    public static double[] gaussKruegerRightHeightToWgs84LongitudeLatitude(double right, double height) {
        if (!(right > 1000000))
            throw new IllegalArgumentException("Invalid Gauss-Krueger right value given: " + right);
        if (!(height > 1000000))
            throw new IllegalArgumentException("Invalid Gauss-Krueger height value given: " + height);

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
        int y0 = new Double(right / 1000000).intValue();
        double L0 = y0 * 3;
        int yInt = new Double(right - y0 * 1000000 - 500000).intValue();
        double B0 = height / alphaGk2Wgs84;
        double Bf = (B0 + betaGk2Wgs84 * Math.sin(2 * B0) + gammaGk2Wgs84 * Math.sin(4 * B0) + deltaGk2Wgs84 * Math.sin(6 * B0) + epsilonGk2Wgs84 * Math.sin(8 * B0));
        double Nf = aBessel / Math.sqrt(1 - e2Bessel * Math.pow(Math.sin(Bf), 2));
        double pif = Math.sqrt(Math.pow(aBessel, 2) / Math.pow(bBessel, 2) * e2Bessel * Math.pow(Math.cos(Bf), 2));
        double tf = Math.tan(Bf);
        double tf1 = tf / 2 / Math.pow(Nf, 2) * (-1 - Math.pow(pif, 2)) * Math.pow(yInt, 2);
        double tf2 = tf / 24 / Math.pow(Nf, 4) * (5 + 3 * Math.pow(tf, 2) + 6 * Math.pow(pif, 2) - 6 * Math.pow(tf, 2) * Math.pow(pif, 2) - 4 * Math.pow(pif, 4) - 9 * Math.pow(tf, 2) * Math.pow(pif, 4)) * Math.pow(yInt, 4);
        // double tf3 = tf / 720 / Math.pow(Nf, 6) * (-61 - 90 * Math.pow(tf, 2) - 45 * Math.pow(tf, 4) - 107 * Math.pow(pif, 2) + 162 * Math.pow(tf, 2) * Math.pow(pif, 2) + 45 * Math.pow(tf, 4) * Math.pow(pif, 2)) * Math.pow(yInt, 6);
        // double tf4 = tf / 40320 / Math.pow(Nf, 8) * (1385 + 3663 * Math.pow(tf, 2) + 4095 * Math.pow(tf, 4) + 1575 * Math.pow(tf, 6)) * Math.pow(yInt, 8);
        double B = (Bf + tf1 + tf2) * 180 / Math.PI;
        double l1 = 1 / Nf / Math.cos(Bf) * yInt;
        double l2 = (1 / Math.pow(Nf, 3) / 6 / Math.cos(Bf)) * (-1 - 2 * Math.pow(tf, 2) - Math.pow(pif, 2)) * Math.pow(yInt, 3);
        // double l3 = 1 / Math.pow(Nf, 5) / 120 / Math.cos(Bf) * (5 + 28 * Math.pow(tf, 2) + 24 * Math.pow(tf, 4) + 6 * Math.pow(pif, 2) + 8 * Math.pow(tf, 2) * Math.pow(pif, 2)) * Math.pow(yInt, 5);
        // double l4 = 1 / Math.pow(Nf, 7) / 15040 / Math.cos(Bf) * (-61 - 622 * Math.pow(tf, 2) - 1320 * Math.pow(tf, 4) - 720 * Math.pow(tf, 6)) * Math.pow(yInt, 7);
        double L = L0 + (l1 + l2) * 180 / Math.PI;

        // Ell. Koordinaten auf dem Bessel-Ellipsoid
        double N = aBessel / Math.sqrt(1 - e2Bessel * Math.pow(Math.sin(B / 180 * Math.PI), 2));
        double x1 = (N + h) * Math.cos(B / 180 * Math.PI) * Math.cos(L / 180 * Math.PI);
        double y1 = (N + h) * Math.cos(B / 180 * Math.PI) * Math.sin(L / 180 * Math.PI);
        double z1 = (N * Math.pow(bBessel, 2) / Math.pow(aBessel, 2) + h) * Math.sin(B / 180 * Math.PI);

        // Rotierte Vektoren
        double x2 = x1 * 1 + y1 * 0.0000119021759 + z1 * 0.000000218166156;
        double y2 = x1 * -0.0000119021759 + y1 * 1 + z1 * -0.000000979323636;
        double z2 = x1 * -0.000000218166156 + y1 * 0.0000009793236 + z1 * 1;

        // Translationen anbringen
        double x = x2 * 0.9999933 + (598.095);
        double y = y2 * 0.9999933 + (73.707);
        double z = z2 * 0.9999933 + (418.197);

        // Vektoren (in ETRF89)
        double s = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double T = Math.atan(z * aWgs84 / (s * bWgs84));
        double B2 = Math.atan((z + e2Wgs84 * Math.pow(aWgs84, 2) / bWgs84 * Math.pow(Math.sin(T), 3)) / (s - e2Wgs84 * aWgs84 * Math.pow(Math.cos(T), 3)));
        double L2 = Math.atan(y / x);
        // double N2 = aWgs84 / Math.sqrt(1 - e2Wgs84 * Math.pow(Math.sin(B2), 2));
        // h = s / Math.cos(B2) - N2;
        double latitude = B2 * 180 / Math.PI;
        double longitude = L2 * 180 / Math.PI;
        return new double[]{longitude, latitude};
    }

    private static final double alphaWgs842Gk = (aBessel + bBessel) / 2 * (1 + Math.pow(nBessel, 2) / 4 + Math.pow(nBessel, 4) / 64);
    private static final double betaWgs842Gk = -3 * nBessel / 2 + 9 * Math.pow(nBessel, 3) / 16 - 3 * Math.pow(nBessel, 5) / 32;
    private static final double gammaWgs842Gk = 15 * Math.pow(nBessel, 2) / 16 - 15 * Math.pow(nBessel, 4) / 32;
    private static final double deltaWgs842Gk = -35 * Math.pow(nBessel, 3) / 48 + 105 * Math.pow(nBessel, 5) / 256;
    private static final double epsilonWgs842Gk = 315 * Math.pow(nBessel, 4) / 512;

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
        double nWgs84 = aWgs84 / Math.sqrt(1 - e2Wgs84 * Math.pow(Math.sin(latitude / 180 * Math.PI), 2));
        double x1 = (nWgs84 + h) * Math.cos(latitude / 180 * Math.PI) * Math.cos(longitude / 180 * Math.PI);
        double y1 = (nWgs84 + h) * Math.cos(latitude / 180 * Math.PI) * Math.sin(longitude / 180 * Math.PI);
        double z1 = (nWgs84 * Math.pow(bWgs84, 2) / Math.pow(aWgs84, 2) + h) * Math.sin(latitude / 180 * Math.PI);

        // Rotierte Vektoren
        double x2 = x1 * 1 + y1 * -0.0000119021759 + z1 * -0.000000218166156;
        double y2 = x1 * 0.0000119021759 + y1 * 1 + z1 * 0.000000979323636;
        double z2 = x1 * 0.000000218166156 + y1 * -0.0000009793236 + z1 * 1;

        // Translationen anbringen
        double x = x2 * 0.9999933 + (-598.095);
        double y = y2 * 0.9999933 + (-73.707);
        double z = z2 * 0.9999933 + (-418.197);

        // Vektoren (in ETRF89)
        double s = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double T = Math.atan(z * aBessel / (s * bBessel));
        double B = Math.atan((z + e2Bessel * Math.pow(aBessel, 2) / bBessel * Math.pow(Math.sin(T), 3)) / (s - e2Bessel * aBessel * Math.pow(Math.cos(T), 3)));
        double L = Math.atan(y / x);
        double N = aBessel / Math.sqrt(1 - e2Bessel * Math.pow(Math.sin(B), 2));
        // h = s / Math.cos(B) - N;
        double B1 = B * 180 / Math.PI;
        double L1 = L * 180 / Math.PI;

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
        double I = (L1 - L0) * Math.PI / 180;
        double B3 = B1 / 180 * Math.PI;
        double pi = Math.sqrt(Math.pow(aBessel, 2) / Math.pow(bBessel, 2) * e2Bessel * Math.pow(Math.cos(B3), 2));
        double t2 = Math.tan(B3);
        double Bogenlaenge = alphaWgs842Gk * (B3 + betaWgs842Gk * Math.sin(2 * B3) + gammaWgs842Gk * Math.sin(4 * B3) + deltaWgs842Gk * Math.sin(6 * B3) + epsilonWgs842Gk * Math.sin(8 * B3));
        double BL1 = t2 / 2 * nWgs84 * Math.pow(Math.cos(B3), 2) * Math.pow(I, 2);
        double BL2 = t2 / 24 * nWgs84 * Math.pow(Math.cos(B3), 4) * (5 - Math.pow(t2, 2) + 9 * Math.pow(pi, 2) + 4 * Math.pow(pi, 4)) * Math.pow(I, 4);
        // double BL3 = t2 / 720 * nWgs84 * Math.pow(Math.cos(B3), 6) * (61 - 58 * Math.pow(t2, 2) - 330 * t2 * Math.pow(pi, 2)) * Math.pow(I, 6);
        // double BL4 = t2 / 40320 * nWgs84 * Math.pow(Math.cos(B3), 8) * (1385 - 3111 * Math.pow(t2, 2) + 543 * Math.pow(t2, 4) - Math.pow(t2, 6)) * Math.pow(I, 8);
        double height = Bogenlaenge + BL1 + BL2;
        double RW1 = N * Math.cos(B3) * I;
        double RW2 = N / 6 * Math.pow(Math.cos(B3), 3) * (1 - Math.pow(t2, 2) + Math.pow(pi, 2)) * Math.pow(I, 3);
        // double RW3 = N / 120 * Math.pow(Math.cos(B3), 5) * (5 - 18 * Math.pow(t2, 2) + Math.pow(t2, 4) + 14 * Math.pow(pi, 2) - 58 * Math.pow(t2, 2) * Math.pow(pi, 2)) * Math.pow(I, 5);
        // double RW4 = N / 5040 * Math.pow(Math.cos(B3), 7) * (61 - 479 * Math.pow(t2, 2) + 179 * Math.pow(t2, 4)) * Math.pow(I, 7);
        double right = RW1 + RW2 + 500000 + L0 / 3 * 1000000;
        return new double[]{right, height};
    }


    public static double bcrAltitudeToElevationMeters(long altitude) {
        double feet = (altitude - ALTITUDE_6m) *
                (meterToFeets(ELEVATION_146m - ELEVATION_6m) / (ALTITUDE_146m - ALTITUDE_6m));
        double meters = feetToMeters(feet) + ELEVATION_6m;
        return roundCeil2(meters);
    }

    public static long elevationMetersToBcrAltitude(double elevation) {
        double feet = meterToFeets(elevation - ELEVATION_6m);
        double altitude = (feet) * ((ALTITUDE_146m - ALTITUDE_6m) / meterToFeets(ELEVATION_146m - ELEVATION_6m));
        altitude += ALTITUDE_6m;
        return (long) Math.floor(altitude);
    }


    public static double ddmm2degrees(double ddmm) {
        double decimal = ddmm / 100.0;
        int asInt = (int) decimal;
        double behindDot = roundFloor7(((decimal - asInt) * 100.0) / 60.0);
        return asInt + behindDot;
    }

    public static double degrees2ddmm(double decimal) {
        int asInt = (int) decimal;
        double behindDot = roundCeil7(decimal - asInt);
        double behindDdMm = roundCeil4(behindDot * 60.0);
        return asInt * 100.0 + behindDdMm;
    }

    private static double roundCeil2(double number) {
        return Math.ceil(number * 100.0) / 100.0;
    }

    private static double roundCeil4(double number) {
        return Math.ceil(number * 10000.0) / 10000.0;
    }

    private static double roundCeil7(double number) {
        return Math.ceil(number * 10000000.0) / 10000000.0;
    }

    private static double roundFloor7(double number) {
        return Math.floor(number * 10000000.0) / 10000000.0;
    }


    public static double feetToMeters(double feet) {
        return feet * METER_OF_A_FEET;
    }

    public static double meterToFeets(double meter) {
        return meter / METER_OF_A_FEET;
    }

    public static double knotsToKilometers(double knots) {
        return knots * KILOMETER_OF_A_KNOT;
    }

    public static double kilometerToKnots(double kilometer) {
        return kilometer / KILOMETER_OF_A_KNOT;
    }

    public static double roundMeterToMillimeterPrecision(double number) {
        return Math.floor(number * 10000.0) / 10000.0;
    }

    public static int ceiling(int dividend, int divisor, boolean roundUpToAtLeastOne) {
        double fraction = (double) dividend / divisor;
        double result = Math.ceil(fraction);
        return Math.max((int) result, roundUpToAtLeastOne ? 1 : 0);
    }


    public static String trim(String string) {
        if (string == null)
            return null;
        string = string.trim();
        if (string == null || string.length() == 0)
            return null;
        else
            return string;
    }

    public static String toMixedCase(String string) {
        StringBuffer buffer = new StringBuffer();
        StringTokenizer tokenizer = new StringTokenizer(string, " -", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.length() > 1)
                buffer.append(token.substring(0, 1).toUpperCase()).append(token.substring(1).toLowerCase());
            else
                buffer.append(token);
        }
        return buffer.toString();
    }

    public static BigDecimal formatDouble(Double aDouble) {
        return aDouble != null ? BigDecimal.valueOf(aDouble) : null;
    }

    public static Double formatDouble(BigDecimal aBigDecimal) {
        return aBigDecimal != null ? aBigDecimal.doubleValue() : null;
    }

    private static final NumberFormat DECIMAL_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
    static {
        DECIMAL_NUMBER_FORMAT.setGroupingUsed(false);
        DECIMAL_NUMBER_FORMAT.setMinimumFractionDigits(1);
        DECIMAL_NUMBER_FORMAT.setMaximumFractionDigits(20);
    }

    public static String formatDoubleAsString(Double aDouble, String nullValue) {
        if (aDouble == null)
            return nullValue;
        return DECIMAL_NUMBER_FORMAT.format(aDouble);
    }

    public static String formatDoubleAsString(Double aDouble) {
        return formatDoubleAsString(aDouble, "0.0");
    }

    public static String formatDoubleAsString(Double aDouble, int fractionCount) {
        StringBuffer buffer = new StringBuffer(formatDoubleAsString(aDouble));
        int index = buffer.indexOf(".");
        if (index == -1) {
            buffer.append(".");
        }
        while (buffer.length() - index <= fractionCount)
            buffer.append("0");
        while (buffer.length() - index > fractionCount + 1)
            buffer.deleteCharAt(buffer.length() - 1);
        return buffer.toString();
    }

    public static String formatIntAsString(Integer anInteger) {
        if (anInteger == null)
            return "0";
        return Integer.toString(anInteger);
    }

    public static Double parseDouble(String string) {
        String trimmed = trim(string);
        if (trimmed != null) {
            trimmed = trimmed.replaceAll(",", ".");
            return Double.parseDouble(trimmed);
        } else
            return null;
    }

    public static Integer parseInt(String string) {
        String trimmed = trim(string);
        if (trimmed != null) {
            if (trimmed.startsWith("+"))
                trimmed = trimmed.substring(1);
            return Integer.parseInt(trimmed);
        } else
            return null;
    }

    public static Long parseLong(String string) {
        String trimmed = trim(string);
        if (trimmed != null) {
            if (trimmed.startsWith("+"))
                trimmed = trimmed.substring(1);
            return Long.parseLong(trimmed);
        } else
            return null;
    }
}
