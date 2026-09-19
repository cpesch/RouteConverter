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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;

/**
 * A compact representation of a calendar, that saves some memory.
 * A {@link Calendar} needs about 250 bytes, this guy needs only 20.
 *
 * @author Christian Pesch
 */

public class CompactCalendar {
    private static final Logger log = Logger.getLogger(CompactCalendar.class.getName());
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long MILLI_SECONDS_PER_DAY = 24 * 60 * 60 * 1000;

    private final Instant instant;
    private final ZoneId zoneId;

    private CompactCalendar(Instant instant, ZoneId zoneId) {
        this.instant = instant;
        this.zoneId = zoneId;
    }

    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private static ZoneId toZoneId(String timeZoneId) {
        if ("UTC".equals(timeZoneId))
            return UTC_ZONE_ID;
        try {
            return ZoneId.of(timeZoneId);
        } catch (DateTimeException e) {
            log.warning("Could not resolve time zone id '" + timeZoneId + "', falling back to UTC");
            return UTC_ZONE_ID;
        }
    }

    // user-facing; locale-sensitive by design
    public static DateFormat createDateFormat(String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        simpleDateFormat.setTimeZone(UTC);
        return simpleDateFormat;
    }

    // user-facing; locale-sensitive by design
    public static DateFormat createDateFormat(String pattern, Locale locale) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, locale);
        simpleDateFormat.setTimeZone(UTC);
        return simpleDateFormat;
    }

    public static CompactCalendar parseDate(String dateString, String dateFormatString) {
        return parseDate(dateString, dateFormatString, true);
    }

    public static CompactCalendar parseDate(String dateString, String dateFormatString, boolean logError) {
        if (dateString == null)
            return null;
        try {
            DateFormat dateFormat = createDateFormat(dateFormatString);
            Date parsed = dateFormat.parse(dateString);
            return fromDate(parsed);
        } catch (ParseException e) {
            log.log(logError ? Level.SEVERE : Level.FINE,
                    "Could not parse '" + dateString + "' with format '" + dateFormatString + "'");
        }
        return null;
    }

    public static CompactCalendar fromMillisAndTimeZone(long timeInMillis, String timeZoneId) {
        return new CompactCalendar(Instant.ofEpochMilli(timeInMillis), toZoneId(timeZoneId));
    }

    public static CompactCalendar fromMillis(long timeInMillis) {
        return fromMillisAndTimeZone(timeInMillis, "UTC");
    }

    public static CompactCalendar fromCalendar(Calendar calendar) {
        return fromMillisAndTimeZone(calendar.getTimeInMillis(), calendar.getTimeZone().getID());
    }

    public static CompactCalendar fromDate(Date date) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTime(date);
        return fromCalendar(calendar);
    }

    public static CompactCalendar now() {
        return fromDate(new Date());
    }

    public CompactCalendar asUTCTimeInTimeZone(TimeZone timeZone) {
        long timeInMillis = getTimeInMillis();
        return new CompactCalendar(Instant.ofEpochMilli(timeInMillis - timeZone.getOffset(timeInMillis)), UTC_ZONE_ID);
    }

    public long getTimeInMillis() {
        return instant.toEpochMilli();
    }

    public String getTimeZoneId() {
        return zoneId.getId();
    }

    public Calendar getCalendar() {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        result.setTimeInMillis(getTimeInMillis());
        return result;
    }

    public boolean hasDateDefined() {
        Calendar calendar = getCalendar();
        return !(calendar.get(YEAR) == 1970 && calendar.get(DAY_OF_YEAR) == 1);
    }

    public Date getTime() {
        return Date.from(instant);
    }

    public boolean after(CompactCalendar other) {
        if (getTimeZoneId().equals(other.getTimeZoneId()))
            return getTimeInMillis() > other.getTimeInMillis();
        return getCalendar().after(other.getCalendar());
    }

    public boolean before(CompactCalendar other) {
        if (getTimeZoneId().equals(other.getTimeZoneId()))
            return getTimeInMillis() < other.getTimeInMillis();
        return getCalendar().before(other.getCalendar());
    }

    public boolean sameDay(CompactCalendar other) {
        return getTimeInMillis() / MILLI_SECONDS_PER_DAY == other.getTimeInMillis()/ MILLI_SECONDS_PER_DAY;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompactCalendar that = (CompactCalendar) o;

        return getTimeInMillis() == that.getTimeInMillis() && getTimeZoneId().equals(that.getTimeZoneId());
    }

    public int hashCode() {
        long timeInMillis = getTimeInMillis();
        int result = (int) (timeInMillis ^ (timeInMillis >>> 32));
        result = 31 * result + getTimeZoneId().hashCode();
        return result;
    }

    public String toString() {
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, MEDIUM);
        format.setTimeZone(TimeZone.getTimeZone(zoneId));
        return format.format(getTime()) + " " + format.getTimeZone().getID();
    }
}
