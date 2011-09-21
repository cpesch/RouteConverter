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

package slash.navigation.converter.gui.helper;

import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.converter.gui.RouteConverter;

import java.text.DateFormat;
import java.util.TimeZone;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.roundFraction;

/**
 * A helper for rendering aspects of {@link BaseNavigationPosition}.
 *
 * @author Christian Pesch
 */

public class PositionHelper {
    public static final DateFormat timeFormat = DateFormat.getDateTimeInstance(SHORT, MEDIUM);
    public static String currentTimeZone = "";

    public static String extractComment(BaseNavigationPosition position) {
        return position.getComment();
    }

    public static String formatElevation(Double elevation) {
        return elevation != null ? round(elevation) + " m" : "";
    }

    public static String extractElevation(BaseNavigationPosition position) {
        return formatElevation(position.getElevation());
    }

    public static String formatLongitudeOrLatitude(Double longitudeOrLatitude) {
        if (isEmpty(longitudeOrLatitude))
            return "";
        String result = Double.toString(longitudeOrLatitude) + " ";
        if (abs(longitudeOrLatitude) < 10.0)
            result = " " + result;
        if (abs(longitudeOrLatitude) < 100.0)
            result = " " + result;
        if (result.length() > 12)
            result = result.substring(0, 12 - 1);
        return result;
    }

    private static String formatSpeed(Double speed) {
        if (isEmpty(speed))
            return "";
        String speedStr;
        if (abs(speed) < 10.0)
            speedStr = Double.toString(roundFraction(speed, 1));
        else
            speedStr = Long.toString(round(speed));
        return speedStr + " Km/h";
    }

    public static String extractSpeed(BaseNavigationPosition position) {
        return formatSpeed(position.getSpeed());
    }

    private static String formatTime(CompactCalendar time) {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        if (!currentTimeZone.equals(timeZonePreference)) {
            timeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentTimeZone = timeZonePreference;
        }
        return timeFormat.format(time.getTime());
    }

    public static String extractTime(BaseNavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatTime(time) : "";
    }
}