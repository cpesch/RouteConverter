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
import slash.navigation.util.Unit;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import java.util.prefs.Preferences;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.String.format;
import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;
import static slash.common.io.CompactCalendar.fromDate;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.roundFraction;
import static slash.navigation.util.Conversion.kilometerToMiles;
import static slash.navigation.util.Conversion.meterToFeets;

/**
 * A helper for rendering aspects of {@link BaseNavigationPosition}.
 *
 * @author Christian Pesch
 */

public class PositionHelper {
    private static final Preferences preferences = Preferences.userNodeForPackage(PositionHelper.class);

    private static final double maximumDistanceDisplayedInMeters = preferences.getDouble("maximumDistanceDisplayedInMeters", 10000.0);
    private static final double maximumDistanceDisplayedInHundredMeters = preferences.getDouble("maximumDistanceDisplayedInHundredMeters", 200000.0);

    private static final DateFormat timeFormat = DateFormat.getDateTimeInstance(SHORT, MEDIUM);
    private static String currentTimeZone = "";

    public static String extractComment(BaseNavigationPosition position) {
        return position.getComment();
    }

    public static String formatDistance(Double distance) {
        if (isEmpty(distance) || distance <= 0.0)
            return "";
        Unit unitPreference = RouteConverter.getInstance().getUnitPreference();
        switch (unitPreference) {
            case METRIC:
                return formatMetricDistance(distance);
            case STATUTE:
                return formatStatuteDistance(distance);
            default:
                throw new IllegalArgumentException(format("Unit %s is not supported", unitPreference));
        }
    }

    private static String formatMetricDistance(Double distance) {
        if (abs(distance) < maximumDistanceDisplayedInMeters)
            return round(distance) + " m";
        if (abs(distance) < maximumDistanceDisplayedInHundredMeters)
            return roundFraction(distance / 1000.0, 1) + " Km";
        return round(distance / 1000.0) + " Km";
    }

    private static String formatStatuteDistance(Double distance) {
        if (abs(distance) < maximumDistanceDisplayedInMeters)
            return round(meterToFeets(distance)) + " ft";
        if (abs(distance) < maximumDistanceDisplayedInHundredMeters)
            return roundFraction(kilometerToMiles(distance / 1000.0), 1) + " mi";
        return round(kilometerToMiles(distance / 1000.0)) + " mi";
    }

    public static String formatElevation(Double elevation) {
        if (isEmpty(elevation))
            return "";
        Unit unitPreference = RouteConverter.getInstance().getUnitPreference();
        switch (unitPreference) {
            case METRIC:
                return round(elevation) + " m";
            case STATUTE:
                return round(meterToFeets(elevation)) + " ft";
            default:
                throw new IllegalArgumentException(format("Unit %s is not supported", unitPreference));
        }
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
        Unit unitPreference = RouteConverter.getInstance().getUnitPreference();
        switch (unitPreference) {
            case METRIC:
                return formatSpeedWithFraction(speed) + " Km/h";
            case STATUTE:
                return formatSpeedWithFraction(kilometerToMiles(speed)) + " mi/h";
            default:
                throw new IllegalArgumentException(format("Unit %s is not supported", unitPreference));
        }
    }

    private static String formatSpeedWithFraction(Double speed) {
        String speedStr;
        if (abs(speed) < 10.0)
            speedStr = Double.toString(roundFraction(speed, 1));
        else
            speedStr = Long.toString(round(speed));
        return speedStr;
    }

    public static String extractSpeed(BaseNavigationPosition position) {
        return formatSpeed(position.getSpeed());
    }

    private static String formatTime(CompactCalendar time) {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        return getTimeFormat(timeZonePreference).format(time.getTime());
    }

    public static String extractTime(BaseNavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatTime(time) : "";
    }

    private static DateFormat getTimeFormat(String timeZonePreference) {
        if (!currentTimeZone.equals(timeZonePreference)) {
            timeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentTimeZone = timeZonePreference;
        }
        return timeFormat;
    }

    static CompactCalendar parseTime(String stringValue, String timeZonePreference) throws ParseException {
        Date parsed = getTimeFormat(timeZonePreference).parse(stringValue);
        return fromDate(parsed);
    }

    public static CompactCalendar parseTime(String stringValue) throws ParseException {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        return parseTime(stringValue, timeZonePreference);
    }
}