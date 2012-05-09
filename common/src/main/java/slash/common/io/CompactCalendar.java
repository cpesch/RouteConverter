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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * A compact representation of a calendar, that saves some memory.
 * A {@link Calendar} needs about 250 bytes, this guy needs only 20.
 *
 * @author Christian Pesch
 */

public class CompactCalendar {
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final long timeInMillis;
    private final String timeZoneId;

    public static CompactCalendar fromMillisAndTimeZone(long timeInMillis, String timeZoneId) {
        return new CompactCalendar(timeInMillis, timeZoneId);
    }

    public static CompactCalendar fromCalendar(Calendar calendar) {
        return fromMillisAndTimeZone(calendar.getTimeInMillis(), calendar.getTimeZone().getID());
    }

    public static CompactCalendar fromDate(Date date) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTime(date);
        return fromCalendar(calendar);
    }


    public static CompactCalendar fromMillis(long timeInMillis) {
        Calendar calendar = Calendar.getInstance(UTC);
        calendar.setTimeInMillis(timeInMillis);
        return fromCalendar(calendar);
    }

    public static CompactCalendar getInstance(String timeZoneId) {
        return fromCalendar(Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)));
    }

    private CompactCalendar(long timeInMillis, String timeZoneId) {
        this.timeInMillis = timeInMillis;
        this.timeZoneId = timeZoneId.equals("UTC") ? "UTC" : timeZoneId.intern();
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

    public Date getTime() {
        return getCalendar().getTime();
    }

    private static volatile Map<String, TimeZone> timeZones = Collections.emptyMap();

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
            Map<String, TimeZone> newTimeZones = new HashMap<String, TimeZone>(timeZones);
            newTimeZones.put(getTimeZoneId(), result);
            newTimeZones = Collections.unmodifiableMap(newTimeZones); // paranoia
            timeZones = newTimeZones;
        }
        return result;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompactCalendar that = (CompactCalendar) o;

        return timeInMillis == that.timeInMillis && !(timeZoneId != null ?
                !timeZoneId.equals(that.timeZoneId) : that.timeZoneId != null);        
    }

    public int hashCode() {
        int result = (int) (timeInMillis ^ (timeInMillis >>> 32));
        result = 31 * result + (timeZoneId != null ? timeZoneId.hashCode() : 0);
        return result;
    }
}
