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
package slash.navigation.converter.gui.panels;

import slash.common.helpers.DateTimeParserException;
import slash.common.helpers.DateTimeParserFormatter;
import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.helpers.PositionHelper;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsModelCallback;
import slash.navigation.converter.gui.models.TimeZoneModel;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;
import static slash.navigation.gui.helpers.WindowHelper.showError;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;
import static slash.navigation.converter.gui.models.PositionColumns.*;

/**
 * Shared implementation from {@link ConvertPanel} and {@link PhotoPanel} for callbacks
 * from the {@link PositionsModel} to other BaseRouteConverter services.
 *
 * @author Christian Pesch
 */

public class PositionsModelCallbackImpl implements PositionsModelCallback {
    private static final Logger log = Logger.getLogger(PositionsModelCallbackImpl.class.getName());

    private final TimeZoneModel timeZoneModel;

    public PositionsModelCallbackImpl(TimeZoneModel timeZoneModel) {
        this.timeZoneModel = timeZoneModel;
    }

    public String getStringAt(NavigationPosition position, int columnIndex) {
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX -> {
                return requireNonNullElse(position.getDescription(), "");
            }
            case DATE_TIME_COLUMN_INDEX -> {
                return extractDateTime(position);
            }
            case DATE_COLUMN_INDEX -> {
                return extractDate(position);
            }
            case TIME_COLUMN_INDEX -> {
                return extractTime(position);
            }
            case LONGITUDE_COLUMN_INDEX -> {
                return PositionHelper.formatLongitude(position.getLongitude());
            }
            case LATITUDE_COLUMN_INDEX -> {
                return PositionHelper.formatLatitude(position.getLatitude());
            }
            case ELEVATION_COLUMN_INDEX -> {
                return PositionHelper.extractElevation(position);
            }
            case SPEED_COLUMN_INDEX -> {
                return PositionHelper.extractSpeed(position);
            }
            case HEADING_COLUMN_INDEX -> {
                return PositionHelper.extractHeading(position);
            }
            case HDOP_COLUMN_INDEX -> {
                return PositionHelper.extractHdop(position);
            }
            case QUALITY_COLUMN_INDEX -> {
                return PositionHelper.extractFixQuality(position);
            }
            case SATELLITES_COLUMN_INDEX -> {
                return PositionHelper.extractSatellites(position);
            }
            case ACCELERATION_X_COLUMN_INDEX -> {
                return PositionHelper.extractAccelerationX(position);
            }
            case ACCELERATION_Y_COLUMN_INDEX -> {
                return PositionHelper.extractAccelerationY(position);
            }
            case ACCELERATION_Z_COLUMN_INDEX -> {
                return PositionHelper.extractAccelerationZ(position);
            }
        }
        throw new IllegalArgumentException("Column " + columnIndex + " does not exist");
    }

    public void setValueAt(NavigationPosition position, int columnIndex, Object value) {
        // If the same string is set that `getStringAt` returns, then do not change anything.
        // ==> It's likely that only in the table cell was clicked and nothing was changed.
        try {
            if (Objects.equals(getStringAt(position, columnIndex), value)) {
                return;
            }
        }
        catch(IllegalArgumentException e) {
            // intentionally left empty
        }

        String string = value != null ? trim(value.toString()) : null;
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX -> position.setDescription(string);
            case DATE_TIME_COLUMN_INDEX -> position.setTime(parseDateTime(value, string, position.getTime()));
            case DATE_COLUMN_INDEX -> position.setTime(parseDate(value, string, position.getTime()));
            case TIME_COLUMN_INDEX -> position.setTime(parseTime(value, string, position.getTime()));
            case LONGITUDE_COLUMN_INDEX -> position.setLongitude(parseLongitude(value, string, position.getLongitude()));
            case LATITUDE_COLUMN_INDEX -> position.setLatitude(parseLatitude(value, string, position.getLatitude()));
            case ELEVATION_COLUMN_INDEX -> position.setElevation(parseElevation(value, string, position.getElevation()));
            case SPEED_COLUMN_INDEX -> position.setSpeed(parseSpeed(value, string, position.getSpeed()));
            default -> {
                // the remaining columns are derived and not editable
            }
        }
    }

    private Double parseLongitude(Object objectValue, String stringValue, Double positionLongitude) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;

        for(DegreeFormat degreeFormat : getDegreeFormats()) {
            try {
                Double value = degreeFormat.parseLongitude(stringValue);
                log.fine(format("Parsed longitude %s with degree format %s to %s", stringValue, degreeFormat, value));
                return value;
            }
            catch (Exception e) {
                // intentionally left empty
            }
        }
        // keeps the previous value instead of throwing so that an invalid entry doesn't crash the EDT
        handleValueParseException(stringValue, "longitude-format-error");
        return positionLongitude;
    }

    private Double parseLatitude(Object objectValue, String stringValue, Double positionLatitude) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;

        for(DegreeFormat degreeFormat : getDegreeFormats()) {
            try {
                Double value = degreeFormat.parseLatitude(stringValue);
                log.fine(format("Parsed latitude %s with degree format %s to %s", stringValue, degreeFormat, value));
                return value;
            }
            catch (Exception e) {
                // intentionally left empty
            }
        }
        // keeps the previous value instead of throwing so that an invalid entry doesn't crash the EDT
        handleValueParseException(stringValue, "latitude-format-error");
        return positionLatitude;
    }

    private Double parseDouble(Object objectValue, String stringValue, String replaceAll) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;
        if (replaceAll != null && stringValue != null)
            stringValue = stringValue.replaceAll(replaceAll, "");
        return Transfer.parseDouble(stringValue);
    }

    private Double parseElevation(Object objectValue, String stringValue, Double positionElevation) {
        for(UnitSystem unitSystem : getUnitSystems()) {
            try {
                Double value = parseDouble(objectValue, stringValue, unitSystem.getElevationName());
                log.fine(format("Parsed elevation %s with unit system %s to %s", stringValue, unitSystem, value));
                return unitSystem.valueToDefault(value);
            }
            catch (Exception e) {
                // intentionally left empty
            }
        }
        // keeps the previous value instead of throwing so that an invalid entry doesn't crash the EDT
        handleValueParseException(stringValue, "elevation-format-error");
        return positionElevation;
    }

    private Double parseSpeed(Object objectValue, String stringValue, Double positionSpeed) {
        for(UnitSystem unitSystem : getUnitSystems()) {
            try {
                Double value = parseDouble(objectValue, stringValue, unitSystem.getSpeedName());
                log.fine(format("Parsed speed %s with unit system %s to %s", stringValue, unitSystem, value));
                return unitSystem.distanceToDefault(value);
            }
            catch (Exception e) {
                // intentionally left empty
            }
        }
        // keeps the previous value instead of throwing so that an invalid entry doesn't crash the EDT
        handleValueParseException(stringValue, "speed-format-error");
        return positionSpeed;
    }

    private String formatDateTime(CompactCalendar time) {
        return getDateTimeFormat().format(time);
    }

    private String extractDateTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatDateTime(time) : "";
    }

    private CompactCalendar parseDateTime(String stringValue, DateTimeParserFormatter formatter, CompactCalendar referenceTimestamp) throws DateTimeParserException {
        try {
            return toCompactCalendar(formatter.parse(stringValue, referenceTimestamp));
        } catch (DateTimeParserException e) {
            // accept the common variant without seconds, e.g. "10:00" for a "...HH:mm:ss" pattern,
            // and only fall back to the original exception when that doesn't match either
            try {
                return toCompactCalendar(formatter.parse(stringValue + ":00", referenceTimestamp));
            } catch (DateTimeParserException ignored) {
                throw e;
            }
        }
    }

    private static CompactCalendar toCompactCalendar(Calendar parsed) {
        return fromMillisAndTimeZone(parsed.getTimeInMillis(), "UTC");
    }

    private CompactCalendar parseDateTime(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        }
        if (stringValue == null) {
            return null;
        }
        try {
            return parseDateTime(stringValue, getDateTimeFormat(), null);
        } catch (DateTimeParserException e) {
            handleDateTimeParseException(stringValue, "date-time-format-error", getDateTimeFormat());
            return positionTime;
        }
    }

    private String formatDate(CompactCalendar time) {
        if(time == null)
            return "?";
        return getDateFormat().format(time);
    }

    private String extractDate(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatDate(time) : "";
    }

    private CompactCalendar parseDate(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        }
        if (stringValue == null) {
            return null;
        }
        try {
            return parseDateTime(stringValue, getDateFormat(), getReferenceTime(positionTime));
        } catch (DateTimeParserException e) {
            handleDateTimeParseException(stringValue, "date-format-error", getDateFormat());
            return positionTime;
        }
    }

    private CompactCalendar getReferenceTime(CompactCalendar positionTime) {
        if (positionTime != null) {
            return positionTime;
        }
        Calendar calendar = Calendar.getInstance(timeZoneModel.getTimeZone());
        calendar.clear(); // prevents "remnants" such as milliseconds
        calendar.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        return fromMillisAndTimeZone(calendar.getTimeInMillis(), "UTC");
    }

    private String formatTime(CompactCalendar time) {
        if(time == null)
            return "?";
        return getTimeFormat().format(time);
    }

    private String extractTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatTime(time) : "";
    }

    private CompactCalendar parseTime(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        }
        if (stringValue == null) {
            return null;
        }
        try {
            return parseDateTime(stringValue, getTimeFormat(), getReferenceTime(positionTime));
        } catch (DateTimeParserException e) {
            handleDateTimeParseException(stringValue, "time-format-error", getTimeFormat());
            return positionTime;
        }
    }

    private DateTimeParserFormatter getDateTimeFormat() {
        String timeZoneId = timeZoneModel.getTimeZoneId();
        return Transfer.getDateTimeFormat(timeZoneId);
    }

    private DateTimeParserFormatter getDateFormat() {
        String timeZoneId = timeZoneModel.getTimeZoneId();
        return Transfer.getDateFormat(timeZoneId);
    }

    private DateTimeParserFormatter getTimeFormat() {
        String timeZoneId = timeZoneModel.getTimeZoneId();
        return Transfer.getTimeFormat(timeZoneId);
    }

    private List<DegreeFormat> getDegreeFormats() {
        DegreeFormat preferred = BaseRouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        return DegreeFormat.getDegreeFormatsWithPreferredDegreeFormat(preferred);
    }

    private List<UnitSystem> getUnitSystems() {
        UnitSystem preferred = BaseRouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        return UnitSystem.getUnitSystemsWithPreferredUnitSystem(preferred);
    }

    private void handleDateTimeParseException(String stringValue, String messageBundleKey, DateTimeParserFormatter format) {
        log.warning(format("Could not parse %s since it does not match the pattern %s", stringValue, format.getPatternInfo()));

        BaseRouteConverter instance = BaseRouteConverter.getInstance();
        if (instance != null) {
            showError(instance.getFrame(),
                    MessageFormat.format(BaseRouteConverter.getBundle().getString(messageBundleKey),
                            stringValue, format.getPatternInfo()), BaseRouteConverter.getTitle());
        }
    }

    private void handleValueParseException(String stringValue, String messageBundleKey) {
        log.warning(format("Could not parse value %s with message bundle key %s", stringValue, messageBundleKey));

        BaseRouteConverter instance = BaseRouteConverter.getInstance();
        if (instance != null) {
            showError(instance.getFrame(),
                    MessageFormat.format(BaseRouteConverter.getBundle().getString(messageBundleKey),
                            stringValue), BaseRouteConverter.getTitle());
        }
    }
}
