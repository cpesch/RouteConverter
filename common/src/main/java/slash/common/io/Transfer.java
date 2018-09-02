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

import slash.common.type.CompactCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.isNaN;
import static java.lang.Integer.toHexString;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static java.util.Locale.US;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Provides value transfer functionality.
 *
 * @author Christian Pesch
 */

public class Transfer {
    private Transfer() {}

    private static final Preferences preferences = Preferences.userNodeForPackage(Transfer.class);
    private static final Logger log = Logger.getLogger(Transfer.class.getName());
    private static final String REDUCE_TIME_TO_SECOND_PRECISION_PREFERENCE = "reduceTimeToSecondPrecision";

    public static final String ISO_LATIN1_ENCODING = "ISO-8859-1";
    public static final String UTF8_ENCODING = "UTF-8";
    public static final String UTF16_ENCODING = "UTF-16";
    public static final String UTF16LE_ENCODING = "UTF-16LE";

    public static double roundFraction(double number, int fractionCount) {
        double factor = pow(10, fractionCount);
        return round(number * factor) / factor;
    }

    public static double ceilFraction(double number, int fractionCount) {
        double factor = pow(10, fractionCount);
        return ceil(number * factor) / factor;
    }

    public static double roundMeterToMillimeterPrecision(double number) {
        return floor(number * 10000.0) / 10000.0;
    }

    public static long roundMillisecondsToSecondPrecision(long number) {
        return (number / 1000) * 1000;
    }

    public static int ceiling(int dividend, int divisor, boolean roundUpToAtLeastOne) {
        double fraction = (double) dividend / divisor;
        double result = ceil(fraction);
        return max((int) result, roundUpToAtLeastOne ? 1 : 0);
    }

    public static int widthInDigits(long number) {
        return 1 + (int) (log(number) / log(10));
    }

    public static String trim(String string) {
        if (string == null)
            return null;
        string = string.trim();
        if (string.length() == 0)
            return null;
        else
            return string;
    }

    public static String trim(String string, int length) {
        string = trim(string);
        if (string == null)
            return null;
        return string.substring(0, min(string.length(), length));
    }

    public static String trimLineFeeds(String string) {
        string = string.replace('\n', ' ');
        string = string.replace('\r', ' ');
        return string;
    }

    public static String toLettersAndNumbers(String string) {
        return string.replaceAll("[^\\w]","");
    }

    public static String toMixedCase(String string) {
        if (string != null && string.toUpperCase().equals(string)) {
            StringBuilder buffer = new StringBuilder();
            StringTokenizer tokenizer = new StringTokenizer(string, " -", true);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (token.length() > 1)
                    buffer.append(token.substring(0, 1).toUpperCase()).append(token.substring(1).toLowerCase());
                else
                    buffer.append(token);
            }
            return buffer.toString();
        } else
            return string;
    }


    public static String escape(String string, char escape, char replacement, String defaultString) {
        String trimmed = trim(string);
        if (trimmed != null)
            trimmed = trimmed.replaceAll("\\" + escape, String.valueOf(replacement));
        else
            trimmed = defaultString;
        return trimmed;
    }

    public static String escape(String string, char escape, char replacement) {
        return escape(string, escape, replacement, "");
    }


    public static boolean isIsoLatin1ButReadWithUtf8(String string) {
        if (string != null) {
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c == '\ufffd')
                    return true;
            }
        }
        return false;
    }

    public static Double formatDouble(BigDecimal aBigDecimal) {
        return aBigDecimal != null ? aBigDecimal.doubleValue() : null;
    }

    public static Integer formatInt(BigInteger aBigInteger) {
        return aBigInteger != null ? aBigInteger.intValue() : null;
    }

    private static final NumberFormat DECIMAL_NUMBER_FORMAT = DecimalFormat.getNumberInstance(US);

    static {
        DECIMAL_NUMBER_FORMAT.setGroupingUsed(false);
        DECIMAL_NUMBER_FORMAT.setMinimumFractionDigits(1);
        DECIMAL_NUMBER_FORMAT.setMaximumFractionDigits(20);
    }

    public static String formatDoubleAsString(Double aDouble) {
        if (aDouble == null || isNaN(aDouble))
            return "0.0";
        return DECIMAL_NUMBER_FORMAT.format(aDouble);
    }

    public static String formatDoubleAsString(Double aDouble, int exactFractionCount) {
        StringBuilder buffer = new StringBuilder(formatDoubleAsString(aDouble));
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

    public static String formatIntAsString(Integer anInteger) {
        if (anInteger == null)
            return "0";
        return Integer.toString(anInteger);
    }

    public static String formatIntAsString(Integer anInteger, int exactDigitCount) {
        StringBuilder buffer = new StringBuilder(formatIntAsString(anInteger));
        while (buffer.length() < exactDigitCount)
            buffer.insert(0, "0");
        return buffer.toString();
    }

    public static BigInteger formatInt(Integer anInteger) {
        if (anInteger == null)
            return null;
        return BigInteger.valueOf(anInteger);
    }

    public static Float formatFloat(Double aDouble) {
        if (aDouble == null)
            return null;
        return aDouble.floatValue();
    }

    public static Double parseDouble(String string) {
        String trimmed = trim(string);
        if (trimmed != null) {
            trimmed = trimmed.replaceAll(",", ".");
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                if (trimmed.equals("\u221e"))
                    return POSITIVE_INFINITY;
                throw e;
            }
        } else
            return null;
    }

    public static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return formatIntAsString((int) hours, 2) + ":" + formatIntAsString((int) minutes % 60, 2) + ":" + formatIntAsString((int) seconds % 60, 2);
    }

    public static Integer parseInteger(String string) {
        String trimmed = trim(string);
        if (trimmed != null) {
            if (trimmed.startsWith("+"))
                trimmed = trimmed.substring(1);
            return Integer.parseInt(trimmed);
        } else
            return null;
    }

    public static int parseInt(String string) {
        Integer integer = parseInteger(string);
        return integer != null ? integer : -1;
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

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static boolean isEmpty(Short aShort) {
        return aShort == null || aShort == 0;
    }

    public static boolean isEmpty(Integer integer) {
        return integer == null || integer == 0;
    }

    public static boolean isEmpty(Long aLong) {
        return aLong == null || aLong == 0;
    }

    public static boolean isEmpty(Double aDouble) {
        return aDouble == null || isNaN(aDouble) || aDouble == 0.0;
    }

    public static boolean isEmpty(BigDecimal bigDecimal) {
        return bigDecimal == null || isEmpty(bigDecimal.doubleValue());
    }

    public static double toDouble(Double aDouble) {
        return aDouble == null || isNaN(aDouble) ? 0.0 : aDouble;
    }

    public static int[] toArray(List<Integer> integers) {
        int[] result = new int[integers.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = integers.get(i);
        }
        return result;
    }

    public static Integer[] toArray(int[] ints) {
        Integer[] result = new Integer[ints.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = ints[i];
        }
        return result;
    }

    public static String encodeUri(String uri) {
        try {
            return URLEncoder.encode(uri, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.severe("Cannot encode uri " + uri + ": " + e);
            return uri;
        }
    }

    public static String encodeUriButKeepSlashes(String uri) {
        return encodeUri(uri).replace("%2F", "/"); // better not .replace("%3A", ":");
    }

    public static String decodeUri(String uri) {
        try {
            return URLDecoder.decode(uri, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.severe("Cannot decode uri " + uri + ": " + e);
            return uri;
        }
    }

    private static final char URI_ESCAPE_CHAR = '%';
    private static final String FORBIDDEN_CHARACTERS = "\\/:*?\"<>|";

    public static String encodeFileName(String name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c == '.' && i == 0) || c == URI_ESCAPE_CHAR || FORBIDDEN_CHARACTERS.indexOf(c) != -1) {
                builder.append(URI_ESCAPE_CHAR);
                if (c < 0x10)
                    builder.append('0');
                builder.append(toHexString(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(SHORT, MEDIUM);
    private static String currentDateTimeTimeZone = "";
    private static final DateFormat dateFormat = DateFormat.getDateInstance(SHORT);
    private static String currentDateTimeZone = "";
    private static final DateFormat timeFormat = DateFormat.getTimeInstance(MEDIUM);
    private static String currentTimeTimeZone = "";

    public synchronized static DateFormat getDateTimeFormat(String timeZonePreference) {
        if (!currentDateTimeTimeZone.equals(timeZonePreference)) {
            dateTimeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentDateTimeTimeZone = timeZonePreference;
        }
        return dateTimeFormat;
    }

    public synchronized static DateFormat getDateFormat(String timeZonePreference) {
        if (!currentDateTimeZone.equals(timeZonePreference)) {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentDateTimeZone = timeZonePreference;
        }
        return dateFormat;
    }

    public synchronized static DateFormat getTimeFormat(String timeZonePreference) {
        if (!currentTimeTimeZone.equals(timeZonePreference)) {
            timeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentTimeTimeZone = timeZonePreference;
        }
        return timeFormat;
    }

    public static CompactCalendar parseXMLTime(XMLGregorianCalendar calendar) {
        if (calendar == null)
            return null;
        GregorianCalendar gregorianCalendar = calendar.toGregorianCalendar(UTC, null, null);
        return fromMillis(gregorianCalendar.getTimeInMillis());
    }

    private static DatatypeFactory datatypeFactory;

    private static synchronized DatatypeFactory getDataTypeFactory() throws DatatypeConfigurationException {
        if (datatypeFactory == null) {
            datatypeFactory = DatatypeFactory.newInstance();
        }
        return datatypeFactory;
    }

    public static XMLGregorianCalendar formatXMLTime(CompactCalendar time) {
       return formatXMLTime(time, preferences.getBoolean(REDUCE_TIME_TO_SECOND_PRECISION_PREFERENCE, false));
    }

    public static XMLGregorianCalendar formatXMLTime(CompactCalendar time, boolean reduceTimeToSecondPrecision) {
        if (time == null)
            return null;
        try {
            GregorianCalendar gregorianCalendar = toUTC(time.getCalendar());
            XMLGregorianCalendar result = getDataTypeFactory().newXMLGregorianCalendar(gregorianCalendar);
            if (reduceTimeToSecondPrecision)
                result.setFractionalSecond(null);
            return result;
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }

    @SuppressWarnings("MagicConstant")
    private static GregorianCalendar toUTC(Calendar calendar) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(UTC, Locale.getDefault());
        gregorianCalendar.clear();
        gregorianCalendar.set(calendar.get(YEAR), calendar.get(MONTH), calendar.get(DATE),
                calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), calendar.get(SECOND));
        gregorianCalendar.set(MILLISECOND, calendar.get(MILLISECOND));
        return gregorianCalendar;
    }

    public static String stripNonValidXMLCharacters(String string) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char current = string.charAt(i);
            if (current == 0x9 || current == 0xA || current == 0xD ||
                    current >= 0x20 && current <= 0xD7FF ||
                    current >= 0xE000 && current <= 0xFFFD)
                buffer.append(current);
        }
        return buffer.toString();
    }
}
