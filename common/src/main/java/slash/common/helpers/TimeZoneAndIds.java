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
package slash.common.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static java.util.Arrays.sort;

/**
 * Provides {@link TimeZoneAndId} helpers.
 *
 * @author Christian Pesch
 */

public class TimeZoneAndIds {
    private static final String ETC = "Etc/";
    private static final String GMT_PLUS = "Etc/GMT+";
    private static final String GMT_MINUS = "Etc/GMT-";

    private static TimeZoneAndIds instance;
    private TimeZoneAndId[] timeZoneAndIds;

    private TimeZoneAndIds() {
        initialize();
    }

    public static TimeZoneAndIds getInstance() {
        if (instance == null) {
            instance = new TimeZoneAndIds();
        }
        return instance;
    }

    private void initialize() {
        String[] ids = TimeZone.getAvailableIDs();

        List<TimeZoneAndId> tzi = new ArrayList<>();
        for (String id : ids) {
            TimeZone timeZone = TimeZone.getTimeZone(id);
            tzi.add(new TimeZoneAndId(modifyId(id), timeZone));
        }

        timeZoneAndIds = filterDuplicates(tzi);

        sort(timeZoneAndIds, new Comparator<TimeZoneAndId>() {
            public int compare(TimeZoneAndId tz1, TimeZoneAndId tz2) {
                return tz1.getId().compareTo(tz2.getId());
            }
        });
    }

    private String modifyId(String id) {
        if (id.contains(GMT_PLUS))
            id = id.replace(GMT_PLUS, GMT_MINUS);
        else if (id.contains(GMT_MINUS))
            id = id.replace(GMT_MINUS, GMT_PLUS);
        if (id.contains(ETC))
            id = id.substring(ETC.length());
        return id;
    }

    private TimeZoneAndId[] filterDuplicates(List<TimeZoneAndId> timeZoneAndIds) {
        List<TimeZoneAndId> result = new ArrayList<>();
        Set<String> foundId = new HashSet<>();
        for(TimeZoneAndId timeZoneAndId : timeZoneAndIds) {
            if(foundId.contains(timeZoneAndId.getId()))
                continue;
            foundId.add(timeZoneAndId.getId());
            result.add(timeZoneAndId);
        }
        return result.toArray(new TimeZoneAndId[0]);
    }

    public TimeZoneAndId[] getTimeZones() {
        return timeZoneAndIds;
    }

    public TimeZoneAndId getTimeZoneAndIdFor(TimeZone timeZone) {
        for (TimeZoneAndId timeZoneAndId : timeZoneAndIds)
            if (timeZoneAndId.getTimeZone().equals(timeZone))
                return timeZoneAndId;
        return null;
    }
}
