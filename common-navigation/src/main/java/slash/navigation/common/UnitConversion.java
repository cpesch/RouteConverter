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

import static slash.common.io.Transfer.ceilFraction;
import static slash.common.io.Transfer.floorFraction;

/**
 * Provides unit conversion functionality.
 *
 * @author Christian Pesch
 */

public class UnitConversion {
    private UnitConversion() {}

    private static final double METER_OF_A_FEET = 0.3048;
    private static final double KILOMETER_OF_A_NAUTIC_MILE = 1.8520043;
    private static final double KILOMETER_OF_A_STATUTE_MILE = 1.609344;
    private static final double METERS_OF_A_KILOMETER = 1000.0;
    private static final double SECONDS_OF_AN_HOUR = 3600.0;

    public static double ddmm2degrees(double ddmm) {
        double decimal = ddmm / 100.0;
        int asInt = (int) decimal;
        double behindDot = floorFraction(((decimal - asInt) * 100.0) / 60.0, 7);
        return asInt + behindDot;
    }

    public static double degrees2ddmm(double decimal) {
        int asInt = (int) decimal;
        double behindDot = ceilFraction(decimal - asInt, 7);
        double behindDdMm = ceilFraction(behindDot * 60.0, 4);
        return asInt * 100.0 + behindDdMm;
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
