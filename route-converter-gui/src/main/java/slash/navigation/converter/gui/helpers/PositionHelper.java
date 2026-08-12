/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.helpers;

import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.ExtendedSensorNavigationPosition;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.nmea.NmeaPosition;

import java.io.File;
import java.util.prefs.Preferences;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.String.format;
import static slash.common.io.Transfer.roundFraction;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.base.WaypointType.Voice;
import static slash.navigation.common.UnitConversion.METERS_OF_A_KILOMETER;

/**
 * A helper for rendering aspects of {@link BaseNavigationPosition}.
 *
 * @author Christian Pesch
 */

public class PositionHelper {
    private static final Preferences preferences = Preferences.userNodeForPackage(PositionHelper.class);

    private static final double maximumDistanceDisplayedInSmallUnit = preferences.getDouble("maximumDistanceDisplayedInSmallUnit", 10000.0);
    private static final double maximumDistanceDisplayedWithFraction = preferences.getDouble("maximumDistanceDisplayedWithFraction", 200.0);

    public static String formatDistance(Double distance) {
        // don't use isEmpty(distance) here since a 0.0 makes sense to display
        if (distance == null || distance <= 0.0)
            return "";
        UnitSystem unitSystem = BaseRouteConverter.getInstance().getUnitSystemModel().getUnitSystem();

        double shortDistanceInUnit = unitSystem.shortDistanceToUnit(distance);
        if (abs(shortDistanceInUnit) < maximumDistanceDisplayedInSmallUnit)
            return format("%d %s", round(shortDistanceInUnit), unitSystem.getShortDistanceName());

        double distanceInUnit = unitSystem.distanceToUnit(distance);
        if (abs(distanceInUnit) < maximumDistanceDisplayedWithFraction)
            return format("%s %s", roundFraction(distanceInUnit, 1), unitSystem.getDistanceName());
        return format("%d %s", round(distanceInUnit), unitSystem.getDistanceName());
    }

    public static String formatElevation(Double elevation) {
        if (elevation == null)
            return "";
        UnitSystem unitSystem = BaseRouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        return formatElevation(elevation, unitSystem);
    }

    // package-private seam: lets PositionHelperTest lock the unit system explicitly,
    // without needing a running BaseRouteConverter instance (GUI is untestable headless)
    static String formatElevation(double elevation, UnitSystem unitSystem) {
        double elevationInUnit = unitSystem.valueToUnit(elevation);
        return format("%s %s", formatRoundedTrimmed(elevationInUnit), unitSystem.getElevationName());
    }

    // Rounds to two fraction digits, then formats without padding: 17.2 -> "17.2", 52.0 -> "52".
    // Transfer#formatDoubleAsString(Double, int) alone would truncate rather than round (1.075 -> "1.07"),
    // so the value is rounded first via roundFraction and only then formatted and trimmed.
    private static String formatRoundedTrimmed(double value) {
        String formatted = Transfer.formatDoubleAsString(roundFraction(value, 2), 2);
        if (formatted.indexOf('.') >= 0) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
    }

    public static String extractElevation(NavigationPosition position) {
        return formatElevation(position.getElevation());
    }

    public static String formatLongitude(Double longitude) {
        if (longitude == null)
            return "";
        DegreeFormat degreeFormat = BaseRouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        return degreeFormat.longitudeToDegrees(longitude);
    }

    public static String formatLatitude(Double latitude) {
        if (latitude == null)
            return "";
        DegreeFormat degreeFormat = BaseRouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        return degreeFormat.latitudeToDegrees(latitude);
    }

    public static String formatSpeed(Double speed) {
        if (speed == null)
            return "";
        UnitSystem unitSystem = BaseRouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        double speedInUnit = unitSystem.distanceToUnit(speed) * METERS_OF_A_KILOMETER;
        if (abs(speedInUnit) < 10.0)
             return format("%s %s", roundFraction(speedInUnit, 1), unitSystem.getSpeedName());
        else
            return format("%d %s", round(speedInUnit), unitSystem.getSpeedName());
    }

    public static String extractSpeed(NavigationPosition position) {
        return formatSpeed(position.getSpeed());
    }

    public static String extractPressure(NavigationPosition position) {
        Double pressure = null;
        if (position instanceof ExtendedSensorNavigationPosition)
            pressure = ((ExtendedSensorNavigationPosition) position).getPressure();
        if(pressure == null)
            return "";
        return format("%d hPa", round(pressure));
    }

    public static String extractTemperature(NavigationPosition position) {
        Double temperature = null;
        if (position instanceof ExtendedSensorNavigationPosition)
            temperature = ((ExtendedSensorNavigationPosition) position).getTemperature();
        if (temperature == null)
            return "";
        return format("%d\u00B0C", round(temperature));
    }

    public static String extractHeartBeat(NavigationPosition position) {
        Short heartBeat = null;
        if (position instanceof ExtendedSensorNavigationPosition)
            heartBeat = ((ExtendedSensorNavigationPosition) position).getHeartBeat();
        if(heartBeat == null)
            return "";
        return format("%d bpm", round(heartBeat));
    }

    public static String formatHeading(Double heading) {
        if (heading == null)
            return "";
        return formatRoundedTrimmed(heading) + "\u00B0";
    }

    public static String extractHeading(NavigationPosition position) {
        Double heading = null;
        if (position instanceof Wgs84Position wgs84Position)
            heading = wgs84Position.getHeading();
        else if (position instanceof NmeaPosition nmeaPosition)
            heading = nmeaPosition.getHeading();
        return formatHeading(heading);
    }

    public static String formatHdop(Double hdop) {
        if (hdop == null)
            return "";
        return Transfer.formatDoubleAsString(hdop, 2);
    }

    public static String extractHdop(NavigationPosition position) {
        Double hdop = null;
        if (position instanceof Wgs84Position wgs84Position)
            hdop = wgs84Position.getHdop();
        else if (position instanceof NmeaPosition nmeaPosition)
            hdop = nmeaPosition.getHdop();
        return formatHdop(hdop);
    }

    public static String formatFixQuality(Integer fixQuality) {
        if (fixQuality == null)
            return "";
        return Transfer.formatIntAsString(fixQuality);
    }

    public static String extractFixQuality(NavigationPosition position) {
        Integer fixQuality = null;
        if (position instanceof Wgs84Position wgs84Position)
            fixQuality = wgs84Position.getFixQuality();
        else if (position instanceof NmeaPosition nmeaPosition)
            fixQuality = nmeaPosition.getFixQuality();
        return formatFixQuality(fixQuality);
    }

    public static String formatAcceleration(Double acceleration) {
        if (acceleration == null)
            return "";
        return Transfer.formatDoubleAsString(acceleration, 2);
    }

    public static String extractAccelerationX(NavigationPosition position) {
        Double acceleration = null;
        if (position instanceof Wgs84Position wgs84Position)
            acceleration = wgs84Position.getAccelerationX();
        return formatAcceleration(acceleration);
    }

    public static String extractAccelerationY(NavigationPosition position) {
        Double acceleration = null;
        if (position instanceof Wgs84Position wgs84Position)
            acceleration = wgs84Position.getAccelerationY();
        return formatAcceleration(acceleration);
    }

    public static String extractAccelerationZ(NavigationPosition position) {
        Double acceleration = null;
        if (position instanceof Wgs84Position wgs84Position)
            acceleration = wgs84Position.getAccelerationZ();
        return formatAcceleration(acceleration);
    }

    // date

    public static String formatDate(CompactCalendar time, String timeZone) {
        if(time == null)
            return "?";
        return Transfer.getDateFormat(timeZone).format(time);
    }

    public static String formatDate(CompactCalendar time) {
        return formatDate(time, BaseRouteConverter.getInstance().getTimeZone().getTimeZoneId());
    }

    public static File extractFile(NavigationPosition position) {
        if (position instanceof Wgs84Position wgs84Position) {
            WaypointType waypointType = wgs84Position.getWaypointType();
            if (waypointType != null && (waypointType.equals(Photo) || waypointType.equals(Voice))) {
                return wgs84Position.getOrigin(File.class);
            }
        }
        return null;
    }
}
