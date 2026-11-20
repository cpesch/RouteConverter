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

    Translated to Java from GPSBabel util.cc (gpsbabel_1_7_0) and jeeps/gpsmath.cc (master 9c1e336)
    for RouteConverter, 2026; modified.
    Copyright (C) 2002-2014 Robert Lipe, robertlipe+source@gpsbabel.org
    Copyright (C) 1999 Alan Bleasby, Copyright (C) 2024 Robert Lipe (gpsmath.cc)
*/

package slash.navigation.gpsbabel;

/**
 * Numeric helpers translated from GPSBabel.
 */
public class GpsBabelMath {
    private static final String[] MONTHS = {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };
    private static final double SEMICIRCLES_PER_HALF_TURN = 2147483648.0; // (double) (1U << 31)

    private GpsBabelMath() {
    }

    /**
     * util.cc si_round: rounds half away from zero.
     */
    public static int siRound(double d) {
        if (d < 0)
            return (int) (d - 0.5);
        return (int) (d + 0.5);
    }

    /**
     * util.cc ddmm2degrees: converts the DDMM.mm format of Magellan and PCX to degrees.
     */
    public static double ddmm2degrees(double ddmm) {
        int deg = (int) (ddmm / 100.0);
        double minutes = (((ddmm / 100.0) - deg) * 100.0) / 60.0;
        return (double) deg + minutes;
    }

    /**
     * util.cc degrees2ddmm: converts degrees to the DDMM.mm format of Magellan and PCX.
     */
    public static double degrees2ddmm(double degrees) {
        int deg = (int) degrees;
        return (deg * 100.0) + ((degrees - deg) * 60.0);
    }

    /**
     * jeeps/gpsmath.cc GPS_Math_Semi_To_Deg.
     */
    public static double semicirclesToDegrees(int semicircles) {
        return (((double) semicircles / SEMICIRCLES_PER_HALF_TURN) * 180.0);
    }

    /**
     * jeeps/gpsmath.cc GPS_Math_Deg_To_Semi: C round(), i.e. half away from zero. Out-of-range
     * values saturate (undefined behaviour in the C++ original).
     */
    public static int degreesToSemicircles(double degrees) {
        double value = (SEMICIRCLES_PER_HALF_TURN / 180.0) * degrees;
        double abs = Math.abs(value);
        double rounded = Math.floor(abs);
        if (abs - rounded >= 0.5)
            rounded += 1.0;
        return (int) (value < 0 ? -rounded : rounded);
    }

    /**
     * util.cc month_lookup: the zero based month number for a case-insensitive three-letter English
     * month name or -1 if unknown.
     */
    public static int monthLookup(String month) {
        if (month == null)
            return -1;
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equalsIgnoreCase(month))
                return i;
        }
        return -1;
    }
}
