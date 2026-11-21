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

    Copyright (C) 2007  Gunar Megger, 0xff@quantentunnel.de
    Copyright (C) 2002-2014 Robert Lipe, robertlipe+source@gpsbabel.org
    Translated to Java from GPSBabel alan.cc (gpsbabel_1_7_0) for RouteConverter, 2026; modified.
*/

package slash.navigation.alan;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static java.math.RoundingMode.HALF_EVEN;
import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static slash.navigation.gpsbabel.GpsBabelMath.siRound;

/**
 * The parts of GPSBabel's alan.cc that the Alan Map 500 waypoints and routes (.wpr) and
 * tracklog (.trl) formats share: scales, labels and the packed date and time.
 *
 * @author Christian Pesch
 */

final class Map500 {
    private static final double PT_SCALE = 36000.0;
    private static final double ALTITUDE_SCALE = 5.0;
    private static final double SPEED_SCALE = 720.0;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int[] MONTH_TO_DAYS = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
    // GPSBabel writes coordinates to GPX with %0.9f and the speed with %.6f
    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_SPEED_DIGITS = 6;

    private Map500() {
    }

    static double pt2deg(int pt) {
        return pt / PT_SCALE;
    }

    static int deg2pt(double degrees) {
        return siRound(degrees * PT_SCALE);
    }

    static double hgt2m(int height) {
        return height / ALTITUDE_SCALE;
    }

    static short m2hgt(double meters) {
        return (short) siRound(meters * ALTITUDE_SCALE);
    }

    static double sp2mps(int speed) {
        return speed / SPEED_SCALE;
    }

    static short mps2sp(double metersPerSecond) {
        return (short) siRound(metersPerSecond * SPEED_SCALE);
    }

    static double asGpxCoordinate(double value) {
        return new BigDecimal(value).setScale(GPX_COORDINATE_DIGITS, HALF_EVEN).doubleValue();
    }

    static double asGpxSpeed(double value) {
        return new BigDecimal(value).setScale(GPX_SPEED_DIGITS, HALF_EVEN).doubleValue();
    }

    /**
     * alan.cc str2lab(): copies the printable ASCII characters among the first len bytes of the
     * UTF-8 encoded source, falls back to fmt with n if none is left and pads with spaces.
     */
    static byte[] str2lab(String source, int length, String format, int n) {
        byte[] result = new byte[length];
        int j = 0;
        if (source != null) {
            byte[] bytes = source.getBytes(UTF_8);
            for (int i = 0; i < length && i < bytes.length && bytes[i] != 0; i++) {
                if (isPrint(bytes[i]))
                    result[j++] = bytes[i];
            }
        }
        if (j == 0 && format != null) {
            // snprintf(dest, len, fmt, n) leaves room for its terminator
            byte[] formatted = String.format(java.util.Locale.ROOT, format, n).getBytes(ISO_8859_1);
            j = Math.min(formatted.length, length - 1);
            System.arraycopy(formatted, 0, result, 0, j);
        }
        if (j < length)
            Arrays.fill(result, j, length, (byte) ' ');
        return result;
    }

    private static boolean isPrint(byte b) {
        return b >= 0x20 && b < 0x7f;
    }

    /**
     * The label without trailing blanks (and NUL bytes if nulIsBlank), cut at the first NUL
     * like xstrndup() does.
     */
    static String fromLabel(byte[] bytes, int offset, int length, boolean nulIsBlank) {
        int j = length - 1;
        while (j >= 0 && (bytes[offset + j] == ' ' || (nulIsBlank && bytes[offset + j] == 0)))
            j--;
        int end = 0;
        while (end < j + 1 && bytes[offset + end] != 0)
            end++;
        return decode(bytes, offset, end);
    }

    /**
     * GPSBabel passes the bytes through to its UTF-8 GPX output; bytes that are no valid UTF-8,
     * like the ISO-8859-1 track names that alan.cc writes, are read as ISO-8859-1.
     */
    static String decode(byte[] bytes, int offset, int length) {
        try {
            return UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, offset, length, ISO_8859_1);
        }
    }

    /**
     * alan.cc pack_time(): the day of month, month and year in the date, the seconds of the day
     * in the time. Times before 1970 are packed as they are: GPSBabel 1.5.4 packs them as
     * 2106-02-07T06:28:15Z, since Qt's toTime_t() returns 0xFFFFFFFF for them, and its C
     * remainder would make the seconds of the day negative.
     */
    static int[] packTime(long seconds) {
        ZonedDateTime dateTime = Instant.ofEpochSecond(seconds).atZone(UTC);
        int date = dateTime.getDayOfMonth() | (dateTime.getMonthValue() << 8) | (dateTime.getYear() << 16);
        int time = (int) Math.floorMod(seconds, SECONDS_PER_DAY);
        return new int[]{date, time};
    }

    /**
     * alan.cc unpack_time() with its leap year arithmetic on C shorts. The result is not cut to
     * 32 bits: GPSBabel 1.5.4 passes it through QDateTime::fromTime_t(uint), which turns a time
     * before 1970 or after 2106-02-07 into one in between.
     * Like alan.cc, the division that truncates towards zero misses leap days before 1968.
     *
     * @return the seconds since the epoch or null if the month is out of range
     */
    static Long unpackTime(int date, int time) {
        short year = (short) ((date >> 16) & 0xffff);
        short month = (short) ((date >> 8) & 0xff);
        short day = (short) (date & 0xff);

        month -= 1;
        year += month / 12;
        if (month < 0) {
            year -= 1;
            month += 12;
        }
        // alan.cc reads behind its table for a month beyond December
        if (month >= MONTH_TO_DAYS.length)
            return null;

        long result = (year - 1970) * 365 + MONTH_TO_DAYS[month];
        if (month <= 1)
            year -= 1;
        result += (year - 1968) / 4;
        result -= (year - 1900) / 100;
        result += (year - 1600) / 400;
        result += day;
        result -= 1;
        result *= SECONDS_PER_DAY;
        result += time;
        return result;
    }
}
