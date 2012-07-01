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
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static slash.common.type.CompactCalendar.UTC;

/**
 * The <code>ISO8601</code> utility class provides helper methods
 * to deal with date/time formatting using a specific ISO8601-compliant
 * format (see <a href="http://www.w3.org/TR/NOTE-datetime">ISO 8601</a>).
 * <p/>
 * The currently supported format is:
 * <pre>
 *   &plusmn;YYYY-MM-DDThh:mm:ss[.SSS]
 * </pre>
 * where:
 * <pre>
 *   &plusmn;YYYY = four-digit year with optional sign where values <= 0 are
 *           denoting years BCE and values > 0 are denoting years CE,
 *           e.g. -0001 denotes the year 2 BCE, 0000 denotes the year 1 BCE,
 *           0001 denotes the year 1 CE, and so on...
 *   MM    = two-digit month (01=January, etc.)
 *   DD    = two-digit day of month (01 through 31)
 *   hh    = two digits of hour (00 through 23) (am/pm NOT allowed)
 *   mm    = two digits of minute (00 through 59)
 *   ss    = two digits of second (00 through 59)
 *   SSS   = optionally: three digits of milliseconds (000 through 999)
 * </pre>
 *
 * @author Unknown
 */

public final class ISO8601 {
    /**
     * misc. numeric formats used in formatting
     */
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
    public static Calendar parse(String text) {
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

        /**
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
            year = Integer.parseInt(text.substring(start, start + 4));
            start += 4;
            // delimiter '-'
            if (text.charAt(start) != '-') {
                return null;
            }
            start++;
            // month (MM)
            month = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter '-'
            if (text.charAt(start) != '-') {
                return null;
            }
            start++;
            // day (DD)
            day = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter 'T'
            if (text.charAt(start) != 'T') {
                return null;
            }
            start++;
            // hour (hh)
            hour = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (text.charAt(start) != ':') {
                return null;
            }
            start++;
            // minute (mm)
            minutes = Integer.parseInt(text.substring(start, start + 2));
            start += 2;
            // delimiter ':'
            if (text.charAt(start) != ':') {
                return null;
            }
            start++;
            // second (ss)
            seconds = Integer.parseInt(text.substring(start, start + 2));
            start += 2;

            // delimiter '.' 'Z' '+' (or 'T')
            char delimiter = text.charAt(start++);
            if (delimiter == '.') {
                // milliseconds (S), (SS), (SSS)
                StringBuilder buffer = new StringBuilder();
                while (true) {
                    delimiter = text.charAt(start++);
                    if (Character.isDigit(delimiter))
                        buffer.append(delimiter);
                    else
                        break;
                }
                while (buffer.length() < 3) {
                    buffer.append('0');
                }
                milliseconds = Integer.parseInt(buffer.toString());
            }

            if (delimiter == 'T')
                delimiter = '+';
            if (delimiter == 'Z') {
                timeZone = CompactCalendar.UTC;
            } else if (delimiter == '+' || delimiter == '-') {
                // delimiter hour (hh)  ':' minute (mm)
                String tzString = text.substring(start, start + 5);
                timeZone = TimeZone.getTimeZone("GMT" + delimiter + tzString);
            } else
                return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        } catch (NumberFormatException e) {
            return null;
        }

        // initialize Calendar object
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setLenient(false);
        // year and era
        if (sign == '-' || year == 0) {
            // not CE, need to set era (BCE) and adjust year
            cal.set(Calendar.YEAR, year + 1);
            cal.set(Calendar.ERA, GregorianCalendar.BC);
        } else {
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.ERA, GregorianCalendar.AD);
        }
        // month (0-based!)
        cal.set(Calendar.MONTH, month - 1);
        // day of month
        cal.set(Calendar.DAY_OF_MONTH, day);
        // hour
        cal.set(Calendar.HOUR_OF_DAY, hour);
        // minute
        cal.set(Calendar.MINUTE, minutes);
        // second
        cal.set(Calendar.SECOND, seconds);
        // millisecond
        cal.set(Calendar.MILLISECOND, milliseconds);

        try {
            /**
             * the following call will trigger an IllegalArgumentException
             * if any of the set values are illegal or out of range
             */
            cal.getTime();
        } catch (IllegalArgumentException e) {
            return null;
        }

        return cal;
    }

    /**
     * Formats a {@link CompactCalendar} value into an ISO8601-compliant date/time string.
     *
     * @param calendar the time value to be formatted into a date/time string
     * @return the formatted date/time string
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static String format(CompactCalendar calendar) {
        if (calendar == null) {
            throw new IllegalArgumentException("argument can not be null");
        }
        return format(calendar.getCalendar(), false);
    }

    /**
     * Formats a {@link Calendar} value into an ISO8601-compliant date/time string.
     *
     * @param calendar            the time value to be formatted into a date/time string
     * @param includeMilliseconds if milli seconds should be included although the spec does not include them
     * @return the formatted date/time string
     * @throws IllegalArgumentException if a <code>null</code> argument is passed
     */
    public static String format(Calendar calendar, boolean includeMilliseconds) {
        if (calendar == null) {
            throw new IllegalArgumentException("argument can not be null");
        }

        // determine era and adjust year if necessary
        int year = calendar.get(Calendar.YEAR);
        if (calendar.isSet(Calendar.ERA) && calendar.get(Calendar.ERA) == GregorianCalendar.BC) {
            /**
             * calculate year using astronomical system:
             * year n BCE => astronomical year -n + 1
             */
            year = 0 - year + 1;
        }

        /**
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
        buffer.append(XX_FORMAT.format(calendar.get(Calendar.MONTH) + 1));
        buffer.append('-');
        // day (DD)
        buffer.append(XX_FORMAT.format(calendar.get(Calendar.DAY_OF_MONTH)));
        buffer.append('T');
        // hour (hh)
        buffer.append(XX_FORMAT.format(calendar.get(Calendar.HOUR_OF_DAY)));
        buffer.append(':');
        // minute (mm)
        buffer.append(XX_FORMAT.format(calendar.get(Calendar.MINUTE)));
        buffer.append(':');
        // second (ss)
        buffer.append(XX_FORMAT.format(calendar.get(Calendar.SECOND)));
        if (includeMilliseconds) {
            // millisecond (SSS)
            buffer.append('.');
            buffer.append(XXX_FORMAT.format(calendar.get(Calendar.MILLISECOND)));
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
