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

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.rint;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static slash.common.io.Transfer.ceilFraction;
import static slash.common.io.Transfer.floorFraction;
import static slash.navigation.common.Orientation.East;
import static slash.navigation.common.Orientation.North;
import static slash.navigation.common.Orientation.South;
import static slash.navigation.common.Orientation.West;

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
    private static final double METERS_OF_A_KILOMETER = 1000.0;
    private static final double SECONDS_OF_AN_HOUR = 3600.0;

    public static Double nmea2degrees(ValueAndOrientation nmea) {
        if(nmea == null)
            return null;
        double decimal = nmea.getValue() / 100.0;
        int asInt = (int) decimal;
        double behindDot = floorFraction(((decimal - asInt) * 100.0) / 60.0, 7);
        double degrees = asInt + behindDot;
        Orientation orientation = nmea.getOrientation();
        boolean southOrWest = orientation.equals(South) || orientation.equals(West);
        return southOrWest ? -degrees : degrees;
    }

    private static ValueAndOrientation position2nmea(Double degrees, Orientation aboveZero, Orientation belowZero) {
        if(degrees == null)
            return null;
        int asInt = (int) degrees.doubleValue();
        double behindDot = ceilFraction(degrees - asInt, 7);
        double behindDdMm = ceilFraction(behindDot * 60.0, 4);
        double ddmm = asInt * 100.0 + behindDdMm;
        double longitude = abs(ddmm);
        Orientation eastOrWest = ddmm >= 0.0 ? aboveZero : belowZero;
        return new ValueAndOrientation(longitude, eastOrWest);
    }

    public static ValueAndOrientation longitude2nmea(Double longitude) {
        return position2nmea(longitude, East, West);
    }

    public static ValueAndOrientation latitude2nmea(Double latitude) {
        return position2nmea(latitude, North, South);
    }


    public static String longitude2ddmm(double longitude) {
        double mm = abs(longitude);
        double dd = floor(mm);
        mm -= dd;
        mm *= 60.0;
        return format(ENGLISH, "%s %.0f° %.3f'", longitude >= 0.0 ? "E" : "W", dd, mm);
    }

    public static String latitude2ddmm(double latitude) {
        double mm = abs(latitude);
        double dd = floor(mm);
        mm -= dd;
        mm *= 60.0;
        return format(ENGLISH, "%s %.0f° %.3f'", latitude >= 0.0 ? "N" : "S", dd, mm);
    }

    public static double ddmm2longitude(String longitude) {
        return -1;
    }

    public static double ddmm2latitude(String latitude) {
        return -1;
    }

    public static String longitude2ddmmss(double longitude) {
        double dd = floor(longitude);
        if (dd < 0)
            dd = dd + 1;

        double fractionAfterDecimal = abs(longitude - dd);
        double secondsWithoutMinutres = fractionAfterDecimal * 3600.0;
        double mm = floor(secondsWithoutMinutres / 60.0);
        double sss = secondsWithoutMinutres - mm * 60.0;

        if (rint(sss) == 60.0) {
            mm = mm + 1;
            sss = 0;
        }

        if (rint(mm) == 60.0) {
            if (dd < 0)
                dd = dd - 1;
            else // ( dd => 0 )
                dd = dd + 1;

            mm = 0;
        }

        return format(ENGLISH, "%s %.0f° %.0f' %.1f\"", longitude >= 0.0 ? "E" : "W", abs(dd), mm, sss);
    }

    public static String latitude2ddmmss(double latitude) {
        double dd = floor(latitude);
        if (dd < 0)
            dd = dd + 1;

        double fractionAfterDecimal = abs(latitude - dd);
        double secondsWithoutMinutres = fractionAfterDecimal * 3600.0;
        double mm = floor(secondsWithoutMinutres / 60.0);
        double sss = secondsWithoutMinutres - mm * 60.0;

        if (rint(sss) == 60.0) {
            mm = mm + 1;
            sss = 0;
        }

        if (rint(mm) == 60.0) {
            if (dd < 0)
                dd = dd - 1;
            else // ( dd => 0 )
                dd = dd + 1;

            mm = 0;
        }

        return format(ENGLISH, "%s %.0f° %.0f' %.1f\"", latitude >= 0.0 ? "N" : "S", abs(dd), mm, sss);
    }

    public static double ddmmss2longitude(String longitude) {
        return -1;
    }

    public static double ddmmss2latitude(String latitude) {
        return -1;
    }

    public static double feetToMeters(double feet) {
        return feet * METER_OF_A_FEET;
    }

    public static double meterToFeets(double meter) {
        return meter / METER_OF_A_FEET;
    }

    public static double nauticMilesToKilometer(double miles) {
        return miles * KILOMETER_OF_A_NAUTIC_MILE;
    }

    public static double kilometerToNauticMiles(double kilometer) {
        return kilometer / KILOMETER_OF_A_NAUTIC_MILE;
    }

    public static double statuteMilesToKilometer(double miles) {
        return miles * KILOMETER_OF_A_STATUTE_MILE;
    }

    public static double kilometerToStatuteMiles(double kilometer) {
        return kilometer / KILOMETER_OF_A_STATUTE_MILE;
    }

    public static double msToKmh(double metersPerSecond) {
        return metersPerSecond * SECONDS_OF_AN_HOUR / METERS_OF_A_KILOMETER;
    }

    public static double kmhToMs(double kilometersPerHour) {
        return kilometersPerHour * METERS_OF_A_KILOMETER / SECONDS_OF_AN_HOUR;
    }
}
