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
import java.util.*;
import java.util.logging.Logger;

import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * A compact representation of a calendar, that saves some memory.
 * A {@link Calendar} needs about 250 bytes, this guy needs only 20.
 *
 * @author Christian Pesch
 */

public class CompactCalendar {
    private static final Logger log = Logger.getLogger(CompactCalendar.class.getName());
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final long timeInMillis;
    private final String timeZoneId;

    private CompactCalendar(long timeInMillis, String timeZoneId) {
        this.timeInMillis = timeInMillis;
        this.timeZoneId = timeZoneId.equals("UTC") ? "UTC" : timeZoneId.intern();
    }

    public static DateFormat createDateFormat(String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        simpleDateFormat.setTimeZone(UTC);
        return simpleDateFormat;
    }

    public static CompactCalendar parseDate(String dateString, String dateFormatString) {
        if (dateString == null)
            return null;
        try {
            DateFormat dateFormat = createDateFormat(dateFormatString);
            Date parsed = dateFormat.parse(dateString);
            return fromDate(parsed);
        } catch (ParseException e) {
            log.severe("Could not parse '" + dateString + "' with format '" + dateFormatString + "'");
        }
        return null;
    }

    public static CompactCalendar fromMillisAndTimeZone(long timeInMillis, String timeZoneId) {
        return new CompactCalendar(timeInMillis, timeZoneId);
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
        return new CompactCalendar(timeInMillis - timeZone.getOffset(timeInMillis), "UTC");
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public Calendar getCalendar() {
        Calendar result = Calendar.getInstance(getTimeZone());
        result.setTimeInMillis(getTimeInMillis());
        return result;
    }

    public boolean hasDateDefined() {
        Calendar calendar = getCalendar();
        return !(calendar.get(YEAR) == 1970 && calendar.get(DAY_OF_YEAR) == 1);
    }

    public Date getTime() {
        return getCalendar().getTime();
    }

    private static volatile Map<String, TimeZone> timeZones = emptyMap();

    private TimeZone getTimeZone() {
        if ("UTC".equals(getTimeZoneId()))
            return UTC;
        // try global read-only map. No synchronization necessary because the field is volatile.
        // (this is only *guaranteed* to work with the Java 5 revised memory model, but works on older JVMs anyway)
        TimeZone result = timeZones.get(getTimeZoneId());
        if (result != null)
            return result;
        synchronized (CompactCalendar.class) {
            // the time zone might have been added while we waited for monitor entry
            result = timeZones.get(getTimeZoneId());
            if (result != null)
                return result;
            // add new timezone to new version of global map.
            // The following call is allegedly expensive (that's why we go through all this trouble)
            result = TimeZone.getTimeZone(getTimeZoneId());
            Map<String, TimeZone> newTimeZones = new HashMap<>(timeZones);
            newTimeZones.put(getTimeZoneId(), result);
            newTimeZones = unmodifiableMap(newTimeZones); // paranoia
            timeZones = newTimeZones;
        }
        return result;
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompactCalendar that = (CompactCalendar) o;

        return timeInMillis == that.timeInMillis && timeZoneId.equals(that.timeZoneId);
    }

    public int hashCode() {
        int result = (int) (timeInMillis ^ (timeInMillis >>> 32));
        result = 31 * result + timeZoneId.hashCode();
        return result;
    }

    public String toString() {
        DateFormat format = DateFormat.getDateTimeInstance(SHORT, MEDIUM);
        format.setTimeZone(getTimeZone());
        return format.format(getTime()) + " " + format.getTimeZone().getID();
    }
}
