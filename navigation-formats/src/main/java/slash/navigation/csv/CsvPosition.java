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

import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.ExtendedSensorNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.excel.ExcelPosition;
import slash.navigation.gpx.GpxPosition;

import java.util.*;
import java.util.function.Consumer;

import static slash.common.io.Transfer.*;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.common.type.ISO8601.parseDate;
import static slash.navigation.base.ExtendedSensorNavigationPosition.transferExtendedSensorData;
import static slash.navigation.csv.ColumnType.*;

/**
 * A position from CSV (.csv) files.
 */

public class CsvPosition extends BaseNavigationPosition implements ExtendedSensorNavigationPosition {

    private record Point(double longitude, double latitude) {
    }

    private static final double FEET_TO_METER = 0.3048;
    private static final double KNOT_TO_KMH = 1.852;

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

    private Point getValueAsPoint(ColumnType type) {
        String value = getValueAsString(type);
        if (value != null) {
            List<Double> parts = Arrays.stream(value.split(","))
                    .map(Transfer::parseDouble)
                    .toList();
            if (parts.size() == 2 && !parts.contains(null)) {
                return new Point(parts.get(1), parts.get(0));
            }
        }
        return null;
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

        if (calendar == null && value != null && isFlightradar24Csv()) {
            Long longValue = parseLong(value);
            if (longValue != null) {
                // Flightradar24 timestamp is in seconds since 1.1.1970
                calendar = fromMillis(longValue * 1000);
            }
        }

        return calendar;
    }

    private void setValueAsString(ColumnType type, String value) {

        Collection<String> existingColumns = new HashSet<>();
        if (rowAsMap.containsKey(type.name())) {
            existingColumns.add(type.name());
        }

        for (String alternativeName : type.getAlternativeNames()) {
            if (rowAsMap.containsKey(alternativeName)) {
                existingColumns.add(alternativeName);
            }
        }

        if (existingColumns.isEmpty()) {
            rowAsMap.put(type.name(), value);
        }
        else {
            existingColumns.forEach(column -> rowAsMap.put(column, value));
        }
    }

    private void setValueAsDouble(ColumnType type, Double value) {
        setValueAsString(type, formatDoubleAsString(value));
    }

    private void setValueAsShort(ColumnType type, Short value) {
        setValueAsString(type, formatShortAsString(value));
    }

    private void setValueAsLong(ColumnType type, Long value) {
        setValueAsString(type, formatLongAsString(value));
    }

    private void setValueAsTime(ColumnType type, CompactCalendar calendar) {
        String value = calendar != null ? createDateFormat(DATE_AND_TIME_FORMAT).format(calendar.getTime().getTime()) : null;
        setValueAsString(type, value);
    }

    private void setValueAsPosition(ColumnType columnType, Point point) {
        if (point != null) {
            setValueAsString(columnType, formatDoubleAsString(point.latitude) + "," + formatDoubleAsString(point.longitude));
        }
        else {
            setValueAsString(columnType, "0.0,0.0");
        }
    }

    public Double getLongitude() {
        Point position = getValueAsPoint(Position);
        Double longitude = getValueAsDouble(Longitude);
        if (position != null && longitude == null) {
            return position.longitude;
        }
        if (position == null && longitude != null) {
            return longitude;
        }
        return null;
    }

    public void setLongitude(Double longitude) {
        if (isFlightradar24Csv()) {
            Point position = getValueAsPoint(Position);
            setValueAsPosition(Position, new Point(longitude, position != null ? position.latitude : 0));
        } else {
            setValueAsDouble(Longitude, longitude);
        }
    }

    public Double getLatitude() {
        Point position = getValueAsPoint(Position);
        Double latitude = getValueAsDouble(Latitude);
        if (position != null && latitude == null) {
            return position.latitude;
        }
        if (position == null && latitude != null) {
            return latitude;
        }
        return null;
    }

    public void setLatitude(Double latitude) {
        if (isFlightradar24Csv()) {
            Point position = getValueAsPoint(Position);
            setValueAsPosition(Position, new Point(position != null ? position.longitude : 0, latitude));
        } else {
            setValueAsDouble(Latitude, latitude);
        }
    }

    public Double getElevation() {
        Double elevation = getValueAsDouble(Elevation);
        if (elevation != null && isFlightradar24Csv()) {
            // Flightradar24 uses Feet
            return elevation * FEET_TO_METER;
        }
        return elevation;
    }

    public void setElevation(Double elevation) {
        setValueAsDouble(Elevation, isFlightradar24Csv() ? elevation / FEET_TO_METER : elevation);
    }

    public CompactCalendar getTime() {
        return getValueAsTime(Time);
    }

    public void setTime(CompactCalendar time) {
        if (isFlightradar24Csv()) {
            setValueAsLong(Time, time.getTimeInMillis() / 1000);
        } else {
            setValueAsTime(Time, time);
        }
    }

    public Double getSpeed() {
        Double speed = getValueAsDouble(Speed);
        if (speed != null && isFlightradar24Csv()) {
            // Flightradar24 uses Knots
            return speed * KNOT_TO_KMH;
        }
        return speed;
    }

    public void setSpeed(Double speed) {
        setValueAsDouble(Speed, isFlightradar24Csv() ? speed / KNOT_TO_KMH : speed);
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

    String getCallSign() {
        return getValueAsString(Callsign);
    }

    private boolean isFlightradar24Csv() {
        return getValueAsString(Callsign) != null;
    }
}
