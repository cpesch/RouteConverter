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
package slash.navigation.csv;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.ExtendedSensorNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.excel.ExcelPosition;
import slash.navigation.gpx.GpxPosition;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import static slash.common.io.Transfer.*;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.common.type.ISO8601.parseDate;
import static slash.navigation.base.ExtendedSensorNavigationPosition.transferExtendedSensorData;
import static slash.navigation.csv.ColumnType.*;

/**
 * A position from CSV (.csv) files.
 */

public class CsvPosition extends BaseNavigationPosition implements ExtendedSensorNavigationPosition {
    private static final String DATE_AND_TIME_FORMAT = "dd.MM.yy HH:mm:ss";
    private static final String DATE_AND_TIME_WITHOUT_SECONDS_FORMAT = "dd.MM.yy HH:mm";

    private final Map<String, String> rowAsMap;

    public CsvPosition(Map<String, String> rowAsMap) {
        this.rowAsMap = rowAsMap;
    }

    public CsvPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        this(new LinkedHashMap<>());
        setLongitude(longitude);
        setLatitude(latitude);
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        setDescription(description);
    }

    Map<String, String> getRowAsMap() {
        return rowAsMap;
    }

    private String getValueAsString(ColumnType type) {
        String value = rowAsMap.get(type.name());
        if (value != null)
            return value;

        for (String alternativeName : type.getAlternativeNames()) {
            value = rowAsMap.get(alternativeName);
            if (value != null)
                return value;
        }
        return null;
    }

    private Double getValueAsDouble(ColumnType type) {
        String value = getValueAsString(type);
        return parseDouble(value);
    }

    private Short getValueAsShort(ColumnType type) {
        String value = getValueAsString(type);
        return parseShort(value);
    }

    private CompactCalendar getValueAsTime(ColumnType type) {
        String value = getValueAsString(type);
        CompactCalendar calendar = parseDate(value, DATE_AND_TIME_FORMAT);
        if (calendar == null)
            calendar = parseDate(value, DATE_AND_TIME_WITHOUT_SECONDS_FORMAT);
        if (calendar == null) {
            Calendar date = parseDate(value);
            if (date != null)
                calendar = fromCalendar(date);
        }
        return calendar;
    }

    private void setValueAsString(ColumnType type, String value) {
        rowAsMap.put(type.name(), value);
    }

    private void setValueAsDouble(ColumnType type, Double value) {
        setValueAsString(type, formatDoubleAsString(value));
    }

    private void setValueAsShort(ColumnType type, Short value) {
        setValueAsString(type, formatShortAsString(value));
    }

    private void setValueAsTime(ColumnType type, CompactCalendar calendar) {
        String value = calendar != null ? createDateFormat(DATE_AND_TIME_FORMAT).format(calendar.getTime().getTime()) : null;
        setValueAsString(type, value);
    }

    public Double getLongitude() {
        return getValueAsDouble(Longitude);
    }

    public void setLongitude(Double longitude) {
        setValueAsDouble(Longitude, longitude);
    }

    public Double getLatitude() {
        return getValueAsDouble(Latitude);
    }

    public void setLatitude(Double latitude) {
        setValueAsDouble(Latitude, latitude);
    }

    public Double getElevation() {
        return getValueAsDouble(Elevation);
    }

    public void setElevation(Double elevation) {
        setValueAsDouble(Elevation, elevation);
    }

    public CompactCalendar getTime() {
        return getValueAsTime(Time);
    }

    public void setTime(CompactCalendar time) {
        setValueAsTime(Time, time);
    }

    public Double getSpeed() {
        return getValueAsDouble(Speed);
    }

    public void setSpeed(Double speed) {
        setValueAsDouble(Speed, speed);
    }

    public Double getPressure() {
        return getValueAsDouble(Pressure);
    }

    public void setPressure(Double pressure) {
        setValueAsDouble(Pressure, pressure);
    }

    public Double getTemperature() {
        return getValueAsDouble(Temperature);
    }

    public void setTemperature(Double temperature) {
        setValueAsDouble(Temperature, temperature);
    }

    public Short getHeartBeat() {
        return getValueAsShort(Heartbeat);
    }

    public void setHeartBeat(Short heartBeat) {
        setValueAsShort(Heartbeat, heartBeat);
    }

    public Double getHeading() {
        return getValueAsDouble(Heading);
    }

    public void setHeading(Double heading) {
        setValueAsDouble(Heading, heading);
    }

    public String getDescription() {
        return getValueAsString(Description);
    }

    public void setDescription(String description) {
        setValueAsString(Description, description);
    }

    public CsvPosition asCsvPosition() {
        return this;
    }

    // handled in CsvRoute#asExcelFormat
    public ExcelPosition asMicrosoftExcelPosition() {
        throw new UnsupportedOperationException();
    }

    public GpxPosition asGpxPosition() {
        GpxPosition position = super.asGpxPosition();
        transferExtendedSensorData(this, position);
        return position;
    }

    public Wgs84Position asWgs84Position() {
        Wgs84Position position = super.asWgs84Position();
        transferExtendedSensorData(this, position);
        return position;
    }
}
