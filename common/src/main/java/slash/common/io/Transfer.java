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

package slash.common.io;

import java.util.prefs.Preferences;
import java.util.Locale;
import java.util.StringTokenizer;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Provides value transfer functionality.
 *
 * @author Christian Pesch
 */

public class Transfer {
    public static final Preferences preferences = Preferences.userNodeForPackage(Transfer.class);

    private Transfer() {}

    public static double roundFraction(double number, int fractionCount) {
        double factor = Math.pow(10, fractionCount);
        return Math.round(number * factor) / factor;
    }

    public static double ceilFraction(double number, int fractionCount) {
        double factor = Math.pow(10, fractionCount);
        return Math.ceil(number * factor) / factor;
    }

    public static double floorFraction(double number, int fractionCount) {
        double factor = Math.pow(10, fractionCount);
        return Math.floor(number * factor) / factor;
    }

    public static double roundMeterToMillimeterPrecision(double number) {
        return Math.floor(number * 10000.0) / 10000.0;
    }

    public static int ceiling(int dividend, int divisor, boolean roundUpToAtLeastOne) {
        double fraction = (double) dividend / divisor;
        double result = Math.ceil(fraction);
        return Math.max((int) result, roundUpToAtLeastOne ? 1 : 0);
    }

    public static int widthInDigits(long number) {
        return 1 + (int) (Math.log(number) / Math.log(10));
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

    public static BigDecimal formatDouble(Double aDouble, int maximumFractionCount) {
        if(aDouble == null)
            return null;
        if(preferences.getBoolean("reduceDecimalPlacesToReasonablePrecision", false))
            aDouble = roundFraction(aDouble, maximumFractionCount);
        return BigDecimal.valueOf(aDouble);
    }

    public static BigDecimal formatPosition(Double longitudeOrLatitude) {
        return formatDouble(longitudeOrLatitude, 7);
    }

    public static BigDecimal formatElevation(Double elevation) {
        return formatDouble(elevation, 2);
    }

    public static BigDecimal formatHeading(Double heading) {
        return formatDouble(heading, 1);
    }

    public static BigDecimal formatSpeed(Double speed) {
        return formatDouble(speed, 2);
    }

    public static Double formatDouble(BigDecimal aBigDecimal) {
        return aBigDecimal != null ? aBigDecimal.doubleValue() : null;
    }

    public static Integer formatInt(BigInteger aBigInteger) {
        return aBigInteger != null ? aBigInteger.intValue() : null;
    }

    private static final NumberFormat DECIMAL_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
    static {
        Transfer.DECIMAL_NUMBER_FORMAT.setGroupingUsed(false);
        Transfer.DECIMAL_NUMBER_FORMAT.setMinimumFractionDigits(1);
        Transfer.DECIMAL_NUMBER_FORMAT.setMaximumFractionDigits(20);
    }

    public static String formatDoubleAsString(Double aDouble) {
        if (aDouble == null)
            return "0.0";
        return DECIMAL_NUMBER_FORMAT.format(aDouble);
    }

    public static String formatDoubleAsString(Double aDouble, int exactFractionCount) {
        StringBuffer buffer = new StringBuffer(formatDoubleAsString(aDouble));
        int index = buffer.indexOf(".");
        if (index == -1) {
            buffer.append(".");
        }
        while (buffer.length() - index <= exactFractionCount)
            buffer.append("0");
        while (buffer.length() - index > exactFractionCount + 1)
            buffer.deleteCharAt(buffer.length() - 1);
        return buffer.toString();
    }

    public static String formatDoubleAsStringWithMaximumFractionCount(Double aDouble, int maximumFractionCount) {
        if(preferences.getBoolean("reduceDecimalPlacesToReasonablePrecision", false))
            aDouble = roundFraction(aDouble, maximumFractionCount);
        return formatDoubleAsString(aDouble);
    }

    public static String formatPositionAsString(Double longitudeOrLatitude) {
        return formatDoubleAsStringWithMaximumFractionCount(longitudeOrLatitude, 7);
    }

    public static String formatElevationAsString(Double elevation) {
        return formatDoubleAsStringWithMaximumFractionCount(elevation, 2);
    }

    public static String formatAccuracyAsString(Double elevation) {
        return formatDoubleAsStringWithMaximumFractionCount(elevation, 6);
    }

    public static String formatHeadingAsString(Double elevation) {
        return formatDoubleAsStringWithMaximumFractionCount(elevation, 1);
    }

    public static String formatSpeedAsString(Double speed) {
        return formatDoubleAsStringWithMaximumFractionCount(speed, 2);
    }

    public static String formatIntAsString(Integer anInteger) {
        if (anInteger == null)
            return "0";
        return Integer.toString(anInteger);
    }

    public static String formatIntAsString(Integer anInteger, int exactDigitCount) {
        StringBuffer buffer = new StringBuffer(formatIntAsString(anInteger));
        while (buffer.length() < exactDigitCount)
            buffer.insert(0, "0");
        return buffer.toString();
    }

    public static BigInteger formatInt(Integer anInteger) {
        if(anInteger == null)
            return null;
        return BigInteger.valueOf(anInteger);
    }

    public static Double parseDouble(String string) {
        String trimmed = trim(string);
        if (trimmed != null) {
            trimmed = trimmed.replaceAll(",", ".");
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                if(trimmed.equals("\u221e"))
                    return Double.POSITIVE_INFINITY;
                throw e;
            }
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
