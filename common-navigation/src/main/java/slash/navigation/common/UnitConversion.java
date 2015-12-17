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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.roundFraction;
import static slash.navigation.common.NavigationConversion.formatDouble;
import static slash.navigation.common.Orientation.*;

/**
 * Provides unit conversion functionality.
 *
 * @author Christian Pesch
 */

public class UnitConversion {
    private UnitConversion() {
    }

    private static final double METER_OF_A_FEET = 0.3048;
    private static final double KILOMETER_OF_A_NAUTIC_MILE = 1.8520043;
    private static final double KILOMETER_OF_A_STATUTE_MILE = 1.609344;
    public static final double METERS_OF_A_KILOMETER = 1000.0;
    private static final int SECONDS_OF_A_MINUTE = 60;
    public static final int SECONDS_OF_AN_HOUR = 60 * SECONDS_OF_A_MINUTE;

    public static Double nmea2degrees(ValueAndOrientation nmea) {
        if(nmea == null)
            return null;
        double decimal = nmea.getValue() / 100.0;
        int asInt = (int) decimal;
        double behindDot = ((decimal - asInt) * 100.0) / SECONDS_OF_A_MINUTE;
        double degrees = asInt + behindDot;
        degrees = roundFraction(degrees, 10);
        Orientation orientation = nmea.getOrientation();
        boolean southOrWest = orientation.equals(South) || orientation.equals(West);
        return southOrWest ? -degrees : degrees;
    }

    private static ValueAndOrientation position2nmea(Double degrees, Orientation aboveZero, Orientation belowZero) {
        if(degrees == null)
            return null;
        int asInt = (int) degrees.doubleValue();
        double behindDot = degrees - asInt;
        double behindDdMm = behindDot * SECONDS_OF_A_MINUTE;
        double ddmm = asInt * 100.0 + behindDdMm;
        double longitude = abs(ddmm);
        longitude = roundFraction(longitude, 10);
        Orientation eastOrWest = ddmm >= 0.0 ? aboveZero : belowZero;
        return new ValueAndOrientation(longitude, eastOrWest);
    }

    public static ValueAndOrientation longitude2nmea(Double longitude) {
        return position2nmea(longitude, East, West);
    }

    public static ValueAndOrientation latitude2nmea(Double latitude) {
        return position2nmea(latitude, North, South);
    }


    private static String coordinate2ddmm(double coordinate, Orientation positive, Orientation negative) {
        double absolute = abs(coordinate);
        double dd = floor(absolute);
        double mm = (absolute - dd) * SECONDS_OF_A_MINUTE;
        return format(ENGLISH, "%s %.0f\u00B0 %.3f'", coordinate >= 0.0 ? positive.value() : negative.value(), dd, mm);
    }

    public static String longitude2ddmm(double longitude) {
        return coordinate2ddmm(longitude, East, West);
    }

    public static String latitude2ddmm(double latitude) {
        return coordinate2ddmm(latitude, North, South);
    }

    private static final Pattern DDMM_TO_COORDINATE = Pattern.
            compile("\\s*(\\w)\\s*" +
                    "([\\d\\.]*)\\s*\u00B0\\s*" +
                    "([\\d\\.]*)\\s*'\\s*");

    private static Double ddmm2coordinate(String coordinateAsDdmm, Orientation negative) {
        if(coordinateAsDdmm != null) {
            Matcher matcher = DDMM_TO_COORDINATE.matcher(coordinateAsDdmm);
            if (matcher.matches()) {
                Orientation orientation = Orientation.fromValue(matcher.group(1));
                Double degree = parseDouble(matcher.group(2));
                Double minutes = parseDouble(matcher.group(3));
                if(degree != null && minutes != null) {
                    double coordinate = degree + (minutes / SECONDS_OF_A_MINUTE);
                    if (orientation != null && orientation.equals(negative))
                        coordinate = -coordinate;
                    return formatDouble(coordinate, 7);
                }
            }
        }
        return null;
    }

    public static Double ddmm2longitude(String longitude) {
        return ddmm2coordinate(longitude, West);
    }

    public static double ddmm2latitude(String latitude) {
        return ddmm2coordinate(latitude, South);
    }

    private static String coordinate2ddmmss(double coordinate, Orientation positive, Orientation negative) {
        double absolute = abs(coordinate);
        double dd = floor(absolute);
        double minutes = (absolute - dd) * SECONDS_OF_A_MINUTE;
        double mm = floor(minutes);
        double sss = (minutes - mm) * SECONDS_OF_A_MINUTE;
        if (rint(sss) == SECONDS_OF_A_MINUTE) {
            mm++;
            sss = 0;
        }
        if (rint(mm) == SECONDS_OF_A_MINUTE) {
            dd++;
            mm = 0;
        }
        return format(ENGLISH, "%s %.0f\u00B0 %.0f' %.3f\"", coordinate >= 0.0 ? positive.value() : negative.value(), dd, mm, sss);
    }

    public static String longitude2ddmmss(double longitude) {
        return coordinate2ddmmss(longitude, East, West);
    }

    public static String latitude2ddmmss(double latitude) {
        return coordinate2ddmmss(latitude, North, South);
    }

    private static final Pattern DDMMSS_TO_COORDINATE = Pattern.
            compile("\\s*(\\w)\\s*" +
                    "([\\d\\.]*)\\s*\u00B0\\s*" +
                    "([\\d\\.]*)\\s*'\\s*" +
                    "([\\d\\.]*)\\s*\"\\s*");

    private static Double ddmmss2coordinate(String coordinateAsDdmmss, Orientation negative) {
        if(coordinateAsDdmmss != null) {
            Matcher matcher = DDMMSS_TO_COORDINATE.matcher(coordinateAsDdmmss);
            if (matcher.matches()) {
                Orientation orientation = Orientation.fromValue(matcher.group(1));
                Double degree = parseDouble(matcher.group(2));
                Double minutes = parseDouble(matcher.group(3));
                Double seconds = parseDouble(matcher.group(4));
                if(degree != null && minutes != null && seconds != null) {
                    double coordinate = degree + (minutes / SECONDS_OF_A_MINUTE) + (seconds / SECONDS_OF_AN_HOUR);
                    if (orientation != null && orientation.equals(negative))
                        coordinate = -coordinate;
                    return formatDouble(coordinate, 7);
                }
            }
        }
        return null;
    }

    public static Double ddmmss2longitude(String longitude) {
        return ddmmss2coordinate(longitude, West);
    }

    public static Double ddmmss2latitude(String latitude) {
        return ddmmss2coordinate(latitude, South);
    }

    public static double feetToMeters(double feet) {
        return feet * METER_OF_A_FEET;
    }

    public static double meterToFeets(double meter) {
        return meter / METER_OF_A_FEET;
    }

    public static double nauticMilesToKiloMeter(double miles) {
        return miles * KILOMETER_OF_A_NAUTIC_MILE;
    }

    public static double kiloMeterToNauticMiles(double kiloMeter) {
        return kiloMeter / KILOMETER_OF_A_NAUTIC_MILE;
    }

    public static double statuteMilesToKiloMeter(double miles) {
        return miles * KILOMETER_OF_A_STATUTE_MILE;
    }

    public static double kiloMeterToStatuteMiles(double kiloMeter) {
        return kiloMeter / KILOMETER_OF_A_STATUTE_MILE;
    }

    public static double msToKmh(double metersPerSecond) {
        return metersPerSecond * SECONDS_OF_AN_HOUR / METERS_OF_A_KILOMETER;
    }

    public static double kmhToMs(double kiloMetersPerHour) {
        return kiloMetersPerHour * METERS_OF_A_KILOMETER / SECONDS_OF_AN_HOUR;
    }
}
