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
package slash.navigation.converter.gui.panels;

import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.PositionHelper;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsModelCallback;
import slash.navigation.converter.gui.models.TimeZoneModel;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Calendar.*;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.converter.gui.helpers.PositionHelper.*;
import static slash.navigation.converter.gui.models.PositionColumns.*;

/**
 * Shared implementation from {@link ConvertPanel} and {@link PhotoPanel} for callbacks
 * from the {@link PositionsModel} to other RouteConverter services.
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
                return position.getDescription();
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
                return formatLongitude(position.getLongitude());
            }
            case LATITUDE_COLUMN_INDEX -> {
                return formatLatitude(position.getLatitude());
            }
            case ELEVATION_COLUMN_INDEX -> {
                return extractElevation(position);
            }
            case SPEED_COLUMN_INDEX -> {
                return extractSpeed(position);
            }
        }
        throw new IllegalArgumentException("Column " + columnIndex + " does not exist");
    }

    public void setValueAt(NavigationPosition position, int columnIndex, Object value) {
        String string = value != null ? trim(value.toString()) : null;
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX -> position.setDescription(string);
            case DATE_TIME_COLUMN_INDEX -> position.setTime(parseDateTime(value, string));
            case DATE_COLUMN_INDEX -> position.setTime(parseDate(value, string, position.getTime()));
            case TIME_COLUMN_INDEX -> position.setTime(parseTime(value, string, position.getTime()));
            case LONGITUDE_COLUMN_INDEX -> position.setLongitude(parseLongitude(value, string));
            case LATITUDE_COLUMN_INDEX -> position.setLatitude(parseLatitude(value, string));
            case ELEVATION_COLUMN_INDEX -> position.setElevation(parseElevation(value, string));
            case SPEED_COLUMN_INDEX -> position.setSpeed(parseSpeed(value, string));
        }
    }

    private Double parseLongitude(Object objectValue, String stringValue) {
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
        // this exception ensures that the editing can continue because the cell value is not cleared
        throw new IllegalArgumentException(format("Could not parse longitude %s", stringValue));
    }

    private Double parseLatitude(Object objectValue, String stringValue) {
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
        // this exception ensures that the editing can continue because the cell value is not cleared
        throw new IllegalArgumentException(format("Could not parse latitude %s", stringValue));
    }

    private Double parseDouble(Object objectValue, String stringValue, String replaceAll) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;
        if (replaceAll != null && stringValue != null)
            stringValue = stringValue.replaceAll(replaceAll, "");
        return Transfer.parseDouble(stringValue);
    }

    private Double parseElevation(Object objectValue, String stringValue) {
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
        // this exception ensures that the editing can continue because the cell value is not cleared
        throw new IllegalArgumentException(format("Could not parse elevation %s", stringValue));
    }

    private Double parseSpeed(Object objectValue, String stringValue) {
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
        // this exception unsures that the editing can continue because the cell value is not cleared
        throw new IllegalArgumentException(format("Could not parse speed %s", stringValue));
    }

    private String formatDateTime(CompactCalendar time) {
        return getDateTimeFormat().format(time.getTime());
    }

    private String extractDateTime(NavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatDateTime(time) : "";
    }

    private CompactCalendar parseDateTime(String stringValue) throws ParseException {
        Date parsed = getDateTimeFormat().parse(stringValue);
        return fromDate(parsed);
    }

    private CompactCalendar parseDateTime(Object objectValue, String stringValue) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                return parseDateTime(stringValue);
            } catch (ParseException e) {
                handleDateTimeParseException(stringValue, "date-time-format-error", getDateTimeFormat());
            }
        }
        return null;
    }

    private CompactCalendar parseDate(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                CompactCalendar result = PositionHelper.parseDate(stringValue);
                if(positionTime != null) {
                    Calendar calendar = positionTime.getCalendar();
                    calendar.set(DAY_OF_MONTH, result.getCalendar().get(DAY_OF_MONTH));
                    calendar.set(MONTH, result.getCalendar().get(MONTH));
                    calendar.set(YEAR, result.getCalendar().get(YEAR));
                    result = fromCalendar(calendar);
                }
                return result;
            } catch (ParseException e) {
                handleDateTimeParseException(stringValue, "date-format-error", getDateFormat());
            }
        }
        return null;
    }

    private CompactCalendar parseTime(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                CompactCalendar result = PositionHelper.parseTime(stringValue);
                if (positionTime != null) {
                    Calendar calendar = positionTime.getCalendar();
                    calendar.set(HOUR_OF_DAY, result.getCalendar().get(HOUR_OF_DAY));
                    calendar.set(MINUTE, result.getCalendar().get(MINUTE));
                    calendar.set(SECOND, result.getCalendar().get(SECOND));
                    result = fromCalendar(calendar);
                }
                return result;
            } catch (ParseException e) {
                handleDateTimeParseException(stringValue, "time-format-error", getTimeFormat());
            }
        }
        return null;
    }

    private DateFormat getDateTimeFormat() {
        String timeZoneId = timeZoneModel.getTimeZoneId();
        return Transfer.getDateTimeFormat(timeZoneId);
    }

    private List<DegreeFormat> getDegreeFormats() {
        DegreeFormat preferred = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        return DegreeFormat.getDegreeFormatsWithPreferredDegreeFormat(preferred);
    }

    private List<UnitSystem> getUnitSystems() {
        UnitSystem preferred = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        return UnitSystem.getUnitSystemsWithPreferredUnitSystem(preferred);
    }

    private void handleDateTimeParseException(String stringValue, String messageBundleKey, DateFormat format) {
        showMessageDialog(RouteConverter.getInstance().getFrame(),
                MessageFormat.format(RouteConverter.getBundle().getString(messageBundleKey),
                        stringValue, extractPattern(format)), RouteConverter.getTitle(), ERROR_MESSAGE);
    }
}
