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

import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.String.format;
import static slash.common.io.Transfer.roundFraction;
import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.base.WaypointType.Voice;

/**
 * A helper for rendering aspects of {@link BaseNavigationPosition}.
 *
 * @author Christian Pesch
 */

public class PositionHelper {
    private static final Preferences preferences = Preferences.userNodeForPackage(PositionHelper.class);

    private static final double maximumDistanceDisplayedInMeters = preferences.getDouble("maximumDistanceDisplayedInMeters", 10000.0);
    private static final double maximumDistanceDisplayedInHundredMeters = preferences.getDouble("maximumDistanceDisplayedInHundredMeters", 200000.0);

    private static final int KILO_BYTE = 1024;
    private static final int MEGA_BYTE = KILO_BYTE * KILO_BYTE;

    public static String formatDistance(Double distance) {
        // don't use isEmpty(distance) here since a 0.0 makes sense to display
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
        double elevationInUnit = unitSystem.valueToUnit(elevation);
        if (abs(elevationInUnit) < 10.0)
            return format("%s %s", roundFraction(elevationInUnit, 1), unitSystem.getElevationName());
        else
            return format("%d %s", round(elevationInUnit), unitSystem.getElevationName());
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

    public static String formatSpeed(Double speed) {
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

    public static String extractTemperature(NavigationPosition position) {
        if (!(position instanceof Wgs84Position))
            return "";
        Double temperature = ((Wgs84Position) position).getTemperature();
        if (temperature == null)
            return "";
        return format("%d\u00B0C", round(temperature));
    }

    public static String extractPressure(NavigationPosition position) {
        if (!(position instanceof Wgs84Position))
            return "";
        Double pressure = ((Wgs84Position) position).getPressure();
        if(pressure == null)
            return "";
        return format("%d hPa", round(pressure));
    }

    public static String extractPattern(DateFormat dateFormat) {
        return dateFormat instanceof SimpleDateFormat ? ((SimpleDateFormat)dateFormat).toLocalizedPattern() : dateFormat.toString();
    }

    // date time

    public static DateFormat getDateTimeFormat() {
        String timeZoneId = RouteConverter.getInstance().getTimeZone().getTimeZoneId();
        return Transfer.getDateTimeFormat(timeZoneId);
    }

    private static String formatDateTime(CompactCalendar time) {
        return getDateTimeFormat().format(time.getTime());
    }

    public static String extractDateTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatDateTime(time) : "";
    }

    public static CompactCalendar parseDateTime(String stringValue) throws ParseException {
        Date parsed = getDateTimeFormat().parse(stringValue);
        return fromDate(parsed);
    }

    // date

    public static DateFormat getDateFormat() {
        String timeZoneId = RouteConverter.getInstance().getTimeZone().getTimeZoneId();
        return Transfer.getDateFormat(timeZoneId);
    }

    public static String formatDate(CompactCalendar time, String timeZone) {
        if(time == null)
            return "?";
        return Transfer.getDateFormat(timeZone).format(time.getTime());
    }

    public static String formatDate(CompactCalendar time) {
        return formatDate(time, RouteConverter.getInstance().getTimeZone().getTimeZoneId());
    }

    public static String extractDate(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatDate(time) : "";
    }

    public static CompactCalendar parseDate(String stringValue) throws ParseException {
        Date parsed = getDateFormat().parse(stringValue);
        return fromDate(parsed);
    }

    // time

    public static DateFormat getTimeFormat() {
        String timeZoneId = RouteConverter.getInstance().getTimeZone().getTimeZoneId();
        return Transfer.getTimeFormat(timeZoneId);
    }

    public static String formatTime(CompactCalendar time, String timeZone) {
        if(time == null)
            return "?";
        return Transfer.getTimeFormat(timeZone).format(time.getTime());
    }

    public static String formatTime(CompactCalendar time) {
        return formatTime(time, RouteConverter.getInstance().getTimeZone().getTimeZoneId());
    }

    public static String extractTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatTime(time) : "";
    }

    public static CompactCalendar parseTime(String stringValue) throws ParseException {
        Date parsed = getTimeFormat().parse(stringValue);
        return fromDate(parsed);
    }


    private static long toNextUnit(Long size, long nextUnit) {
        return round(size / (double) nextUnit + 0.5);
    }

    public static String formatSize(Long size) {
        if(size == null)
            return "?";

        String unit;
        if (size > 2 * MEGA_BYTE) {
            size = toNextUnit(size, MEGA_BYTE);
            unit = "MByte";
        } else if (size > 2 * KILO_BYTE) {
            size = toNextUnit(size, KILO_BYTE);
            unit = "kByte";
        } else {
            unit = "Bytes";
        }
        return format("%d %s", size, unit);
    }

    public static File extractFile(NavigationPosition position) {
        if (position instanceof Wgs84Position) {
            Wgs84Position wgs84Position = (Wgs84Position) position;
            WaypointType waypointType = wgs84Position.getWaypointType();
            if (waypointType != null && (waypointType.equals(Photo) || waypointType.equals(Voice))) {
                File file = wgs84Position.getOrigin(File.class);
                if (file != null) {
                    return file;
                }
            }
        }
        return null;
    }
}