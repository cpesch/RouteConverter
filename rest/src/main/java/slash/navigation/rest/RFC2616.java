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
package slash.navigation.rest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static java.util.Locale.US;
import static java.util.TimeZone.getTimeZone;
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;

/**
 * The <code>RFC2616</code> utility class provides helper methods
 * to deal with date/time formatting
 * see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3">RFC2616</a>).
 *
 * @author Christian Pesch
 */

public class RFC2616 {
    private static final String RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final TimeZone GMT = getTimeZone("GMT");

    private static DateFormat createDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat(RFC1123_DATE, US);
        // django.utils.http.RFC1123_DATE expects GMT
        format.setTimeZone(GMT);
        return format;
    }

    public static String formatDate(long date) {
        return createDateFormat().format(new Date(date));
    }

    public static Calendar parseDate(String date) throws ParseException {
        Date intermediate = createDateFormat().parse(date);
        return fromMillisAndTimeZone(intermediate.getTime(), GMT.getID()).getCalendar();
    }
}
