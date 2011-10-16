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
        Unit unit = RouteConverter.getInstance().getUnitModel().getCurrent();
        double distanceInMeters = convertMetersToUnit(distance, unit);
        if (abs(distanceInMeters) < maximumDistanceDisplayedInMeters)
            return format("%d %s", round(distanceInMeters), unit.getElevation());
        double distanceInKilometers = convertKilometersToUnit(distance / 1000.0, unit);
        if (abs(distanceInMeters) < maximumDistanceDisplayedInHundredMeters)
            return format("%s %s", roundFraction(distanceInKilometers, 1), unit.getDistance());
        return format("%d %s", round(distanceInKilometers), unit.getDistance());
    }

    private static double convertMetersToUnit(Double value, Unit unit) {
        switch (unit) {
            case METRIC:
                return value;
            case STATUTE:
                return meterToFeets(value);
            default:
                throw new IllegalArgumentException(format("Unit %s is not supported", unit));
        }
    }

    public static String formatElevation(Double elevation) {
        if (isEmpty(elevation))
            return "";
        Unit unit = RouteConverter.getInstance().getUnitModel().getCurrent();
        double distanceInUnit = convertMetersToUnit(elevation, unit);
        return format("%d %s", round(distanceInUnit), unit.getElevation());
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

    private static double convertKilometersToUnit(Double value, Unit unit) {
        switch (unit) {
            case METRIC:
                return value;
            case STATUTE:
                return kilometerToMiles(value);
            default:
                throw new IllegalArgumentException(format("Unit %s is not supported", unit));
        }
    }

    private static String formatSpeed(Double speed) {
        if (isEmpty(speed))
            return "";
        Unit unit = RouteConverter.getInstance().getUnitModel().getCurrent();
        double speedInUnit = convertKilometersToUnit(speed, unit);
        if (abs(speedInUnit) < 10.0)
             return format("%s %s/h", roundFraction(speedInUnit, 1), unit.getDistance());
        else
            return format("%d %s/h", round(speedInUnit), unit.getDistance());
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