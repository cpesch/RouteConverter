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

package slash.common.type;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static java.util.Calendar.*;
import static java.util.GregorianCalendar.AD;
import static java.util.GregorianCalendar.BC;
import static java.util.TimeZone.getTimeZone;
import static slash.common.type.CompactCalendar.UTC;

/**
 * The <code>ISO8601</code> utility class provides helper methods
 * to deal with date/time formatting using a specific ISO8601-compliant
 * format (see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601</a>).
 *
 * The currently supported format is:
 * <pre>
 *   +-YYY-MM-DDThh:mm:ss[.SSS]TZD
 * </pre>
 * where:
 * <pre>
 *  +-YYYY = four-digit year with optional sign where values ;lteq; 0 are
 *           denoting years BCE and values &gt; 0 are denoting years CE,
 *           e.g. -0001 denotes the year 2 BCE, 0000 denotes the year 1 BCE,
 *           0001 denotes the year 1 CE, and so on...
 *   MM    = two-digit month (01=January, etc.)
 *   DD    = two-digit day of month (01 through 31)
 *   hh    = two digits of hour (00 through 23) (am/pm NOT allowed)
 *   mm    = two digits of minute (00 through 59)
 *   ss    = two digits of second (00 through 59)
 *   SSS   = optionally: three digits of milliseconds (000 through 999)
 *   TZD   = time zone designator (Z or +hh:mm or -hh:mm)
 * </pre>
 *
 * @author Unknown
 */

public final class ISO8601 {
    private static final DecimalFormat XX_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat XXX_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat XXXX_FORMAT = new DecimalFormat("0000");

    /**
     * Parses an ISO8601-compliant date/time string.
     *
     * @param text the date/time string to be parsed
     * @return a <code>Calendar</code>, or <code>null</code> if the input could
     *         not be parsed
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static Calendar parseDate(String text) {
        if (text == null) {
            throw new IllegalArgumentException("argument can not be null");
        }

        // check optional leading sign
        char sign;
        int start;
        if (text.startsWith("-")) {
            sign = '-';
            start = 1;
        } else if (text.startsWith("+")) {
            sign = '+';
            start = 1;
        } else {
            sign = '+'; // no sign specified, implied '+'
            start = 0;
        }

        /*
         * the expected format of the remainder of the string is:
         * YYYY-MM-DDThh:mm:ss
         *
         * note that we cannot use java.text.SimpleDateFormat for
         * parsing because it can't handle years <= 0 and TZD's
         */

        TimeZone timeZone;
        int year, month, day, hour, minutes, seconds, milliseconds = 0;
        try {
            // year (YYYY)
            year = parseInt(text.substring(start, start + 4));
            start += 4;
            // delimiter '-'
            if (text.charAt(start) != '-') {
                return null;
            }
            start++;
            // month (MM)
            month = parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter '-'
            if (text.charAt(start) != '-') {
                return null;
            }
            start++;
            // day (DD)
            day = parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter 'T'
            if (text.charAt(start) != 'T') {
                return null;
            }
            start++;
            // hour (hh)
            hour = parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (text.charAt(start) != ':') {
                return null;
            }
            start++;
            // minute (mm)
            minutes = parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (text.charAt(start) != ':') {
                return null;
            }
            start++;
            // second (ss)
            seconds = parseInt(text.substring(start, start + 2));
            start += 2;

            // delimiter '.' 'Z' '+' (or 'T')
            char delimiter = text.charAt(start++);
            if (delimiter == '.') {
                // milliseconds (S), (SS), (SSS)
                StringBuilder buffer = new StringBuilder();
                while (true) {
                    delimiter = text.charAt(start++);
                    if (isDigit(delimiter))
                        buffer.append(delimiter);
                    else
                        break;
                }
                while (buffer.length() < 3) {
                    buffer.append('0');
                }
                milliseconds = parseInt(buffer.toString());
            }

            if (delimiter == 'T')
                delimiter = '+';
            if (delimiter == 'Z') {
                timeZone = UTC;
            } else if (delimiter == '+' || delimiter == '-') {
                // delimiter hour (hh) ':' minute (mm)
                String tzString = text.substring(start, start + 5);
                timeZone = getTimeZone("GMT" + delimiter + tzString);
            } else
                return null;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return null;
        }

        // initialize Calendar object
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setLenient(false);
        // year and era
        if (sign == '-' || year == 0) {
            // not CE, need to set era (BCE) and adjust year
            calendar.set(YEAR, year + 1);
            calendar.set(ERA, BC);
        } else {
            calendar.set(YEAR, year);
            calendar.set(ERA, AD);
        }
        // month (0-based!)
        calendar.set(MONTH, month - 1);
        // day of month
        calendar.set(DAY_OF_MONTH, day);
        // hour
        calendar.set(HOUR_OF_DAY, hour);
        // minute
        calendar.set(MINUTE, minutes);
        // second
        calendar.set(SECOND, seconds);
        // millisecond
        calendar.set(MILLISECOND, milliseconds);

        try {
            /*
             * the following call will trigger an IllegalArgumentException
             * if any of the set values are illegal or out of range
             */
            calendar.getTime();
        } catch (IllegalArgumentException e) {
            return null;
        }

        return calendar;
    }

    /**
     * Formats a {@link CompactCalendar} value into an ISO8601-compliant date/time string.
     *
     * @param calendar the time value to be formatted into a date/time string
     * @return the formatted date/time string
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static String formatDate(CompactCalendar calendar) {
        if (calendar == null) {
            throw new IllegalArgumentException("argument can not be null");
        }
        return formatDate(calendar.getCalendar(), false);
    }

    /**
     * Formats a {@link Calendar} value into an ISO8601-compliant date/time string.
     *
     * @param calendar            the time value to be formatted into a date/time string
     * @param includeMilliseconds if milli seconds should be included although the spec does not include them
     * @return the formatted date/time string
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static String formatDate(Calendar calendar, boolean includeMilliseconds) {
        if (calendar == null) {
            throw new IllegalArgumentException("argument can not be null");
        }

        // determine era and adjust year if necessary
        int year = calendar.get(YEAR);
        if (calendar.isSet(ERA) && calendar.get(ERA) == BC) {
            /*
             * calculate year using astronomical system:
             * year n BCE => astronomical year -n + 1
             */
            year = 0 - year + 1;
        }

        /*
         * the format of the date/time string is:
         * YYYY-MM-DDThh:mm:ss
         *
         * note that we cannot use java.text.SimpleDateFormat for
         * formatting because it can't handle years <= 0 and TZD's
         */
        StringBuilder buffer = new StringBuilder();
        // year ([-]YYYY)
        buffer.append(XXXX_FORMAT.format(year));
        buffer.append('-');
        // month (MM)
        buffer.append(XX_FORMAT.format(calendar.get(MONTH) + 1));
        buffer.append('-');
        // day (DD)
        buffer.append(XX_FORMAT.format(calendar.get(DAY_OF_MONTH)));
        buffer.append('T');
        // hour (hh)
        buffer.append(XX_FORMAT.format(calendar.get(HOUR_OF_DAY)));
        buffer.append(':');
        // minute (mm)
        buffer.append(XX_FORMAT.format(calendar.get(MINUTE)));
        buffer.append(':');
        // second (ss)
        buffer.append(XX_FORMAT.format(calendar.get(SECOND)));
        if (includeMilliseconds) {
            // millisecond (SSS)
            buffer.append('.');
            buffer.append(XXX_FORMAT.format(calendar.get(MILLISECOND)));
        }
        if (calendar.getTimeZone().equals(UTC))
            buffer.append('Z');
        else {
            buffer.append('+');
            int offsetHours = calendar.getTimeZone().getRawOffset() / 1000 / 3600;
            int offsetMinutes = calendar.getTimeZone().getRawOffset() / 1000 / 60 - offsetHours * 60;
            buffer.append(XX_FORMAT.format(offsetHours));
            buffer.append(':');
            buffer.append(XX_FORMAT.format(offsetMinutes));
        }
        return buffer.toString();
    }
}
