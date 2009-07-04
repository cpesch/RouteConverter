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

package slash.navigation.util;

import java.util.*;

/**
 * A compact representation of a calendar, that saves some memory.
 * A {@link Calendar} needs about 250 bytes, this guy needs only 20.
 *
 * @author Christian Pesch
 */

public class CompactCalendar {
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private long timeInMillis;
    private String timeZoneId;

    public static CompactCalendar fromCalendar(Calendar calendar) {
        return new CompactCalendar(calendar.getTimeInMillis(), calendar.getTimeZone().getID());
    }

    public static CompactCalendar getInstance() {
        return fromCalendar(Calendar.getInstance());
    }

    private CompactCalendar(long timeInMillis, String timeZoneId) {
        this.timeInMillis = timeInMillis;
        this.timeZoneId = timeZoneId.equals("GMT") ? "GMT" : timeZoneId.intern();
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
        if ("GMT".equals(timeZoneId))
            return GMT;
        // try global read-only map. No synchronization necessary because the field is volatile.
        // (this is only *guaranteed* to work with the Java 5 revised memory model, but works on older JVMs anyway)
        TimeZone result = timeZones.get(timeZoneId);
        if (result != null)
            return result;
        synchronized (CompactCalendar.class) {
            // the time zone might have been added while we waited for monitor entry
            result = timeZones.get(timeZoneId);
            if (result != null)
                return result;
            // add new timezone to new version of global map.
            // The following call is allegedly expensive (that's why we go through all this trouble)
            result = TimeZone.getTimeZone(timeZoneId);
            Map<String, TimeZone> newTimeZones = new HashMap<String, TimeZone>(timeZones);
            newTimeZones.put(timeZoneId, result);
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
