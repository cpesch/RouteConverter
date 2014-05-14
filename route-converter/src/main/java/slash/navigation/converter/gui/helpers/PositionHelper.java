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

package slash.navigation.converter.gui.helpers;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;

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
import static slash.common.io.Transfer.roundFraction;
import static slash.common.type.CompactCalendar.fromDate;

/**
 * A helper for rendering aspects of {@link BaseNavigationPosition}.
 *
 * @author Christian Pesch
 */

public class PositionHelper {
    private static final Preferences preferences = Preferences.userNodeForPackage(PositionHelper.class);

    private static final double maximumDistanceDisplayedInMeters = preferences.getDouble("maximumDistanceDisplayedInMeters", 10000.0);
    private static final double maximumDistanceDisplayedInHundredMeters = preferences.getDouble("maximumDistanceDisplayedInHundredMeters", 200000.0);

    private static final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(SHORT, MEDIUM);
    private static final DateFormat timeFormat = DateFormat.getTimeInstance(MEDIUM);
    private static String currentTimeZone = "";

    public static String formatDistance(Double distance) {
        if (distance == null || distance <= 0.0)
            return "";
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        double distanceInMeters = unitSystem.valueToUnit(distance);
        if (abs(distanceInMeters) < maximumDistanceDisplayedInMeters)
            return format("%d %s", round(distanceInMeters), unitSystem.getElevationName());
        double distanceInKiloMeters = unitSystem.distanceToUnit(distance / 1000.0);
        if (abs(distanceInMeters) < maximumDistanceDisplayedInHundredMeters)
            return format("%s %s", roundFraction(distanceInKiloMeters, 1), unitSystem.getDistanceName());
        return format("%d %s", round(distanceInKiloMeters), unitSystem.getDistanceName());
    }

    public static String formatElevation(Double elevation) {
        if (elevation == null)
            return "";
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        double distanceInUnit = unitSystem.valueToUnit(elevation);
        return format("%d %s", round(distanceInUnit), unitSystem.getElevationName());
    }

    public static String extractElevation(NavigationPosition position) {
        return formatElevation(position.getElevation());
    }

    public static String formatLongitude(Double longitude) {
        if (longitude == null)
            return "";
        DegreeFormat degreeFormat = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        return degreeFormat.longitudeToDegrees(longitude);
    }

    public static String formatLatitude(Double latitude) {
        if (latitude == null)
            return "";
        DegreeFormat degreeFormat = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        return degreeFormat.latitudeToDegrees(latitude);
    }

    private static String formatSpeed(Double speed) {
        if (speed == null)
            return "";
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        Double speedInUnit = unitSystem.distanceToUnit(speed);
        if (abs(speedInUnit) < 10.0)
             return format("%s %s", roundFraction(speedInUnit, 1), unitSystem.getSpeedName());
        else
            return format("%d %s", round(speedInUnit), unitSystem.getSpeedName());
    }

    public static String extractSpeed(NavigationPosition position) {
        return formatSpeed(position.getSpeed());
    }

    private static DateFormat getDateTimeFormat(String timeZonePreference) {
        if (!currentTimeZone.equals(timeZonePreference)) {
            dateTimeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentTimeZone = timeZonePreference;
        }
        return dateTimeFormat;
    }

    private static DateFormat getTimeFormat(String timeZonePreference) {
        if (!currentTimeZone.equals(timeZonePreference)) {
            timeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentTimeZone = timeZonePreference;
        }
        return timeFormat;
    }

    public static String formatDate(CompactCalendar time) {
        DateFormat dateFormat = DateFormat.getDateInstance(SHORT);
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
        return dateFormat.format(time.getTime());
    }

    private static String formatDateTime(CompactCalendar time) {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        return getDateTimeFormat(timeZonePreference).format(time.getTime());
    }

    private static String formatTime(CompactCalendar time) {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        return getTimeFormat(timeZonePreference).format(time.getTime());
    }

    public static String extractDateTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatDateTime(time) : "";
    }

    public static String extractTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatTime(time) : "";
    }

    static CompactCalendar parseDateTime(String stringValue, String timeZonePreference) throws ParseException {
        Date parsed = getDateTimeFormat(timeZonePreference).parse(stringValue);
        return fromDate(parsed);
    }

    static CompactCalendar parseTime(String stringValue, String timeZonePreference) throws ParseException {
        Date parsed = getTimeFormat(timeZonePreference).parse(stringValue);
        return fromDate(parsed);
    }

    public static CompactCalendar parseDateTime(String stringValue) throws ParseException {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        return parseDateTime(stringValue, timeZonePreference);
    }

    public static CompactCalendar parseTime(String stringValue) throws ParseException {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        return parseTime(stringValue, timeZonePreference);
    }
}