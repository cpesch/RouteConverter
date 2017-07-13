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

package slash.navigation.columbus;

import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.io.File;
import java.io.PrintWriter;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static slash.common.io.Transfer.escape;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteComments.isPositionDescription;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;

/**
 * Reads and writes Columbus GPS Type 2 (.csv) files.
 *
 * Type A:
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,PRES,TEMP
 * Format: 7,T,160325,151927,26.097885N,119.265160E,-25,39.3,83,1020.2,17
 *
 * Type B:
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING
 * Format: 7,T,160325,151927,26.097885N,119.265160E,-25,39.3,83
 *
 * @author Christian Pesch
 */

public class ColumbusGpsType2Format extends ColumbusGpsFormat {
    private static final String COMMON_HEADER = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING";
    private static final String TYPE_A_HEADER = ",PRES,TEMP";
    private static final String HEADER_LINE = COMMON_HEADER + TYPE_A_HEADER;
    private static final Pattern HEADER_PATTERN = Pattern.compile(COMMON_HEADER + "(" + TYPE_A_HEADER + ")?");
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([" + VALID_TAG_VALUES + "])" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "(\\d*)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "(\\d*)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([NS])" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([WE])" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([-\\d]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO +
                    "(" + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([-\\d]+)" + SPACE_OR_ZERO + SEPARATOR + "?" +
                    SPACE_OR_ZERO + "([^" + SEPARATOR + "]*)" + SPACE_OR_ZERO +
                    ")?" +
                    END_OF_LINE);

    public String getName() {
        return "Columbus GPS Type 2 (*" + getExtension() + ")";
    }

    protected Pattern getLinePattern() {
        return LINE_PATTERN;
    }


    protected boolean hasValidFix(String line, Matcher matcher) {
        return true;
    }

    protected String getHeader() {
        return HEADER_LINE;
    }

    protected Pattern getHeaderPattern() {
        return HEADER_PATTERN;
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        WaypointType waypointType = parseTag(trim(lineMatcher.group(2)));
        String date = lineMatcher.group(3);
        String time = lineMatcher.group(4);
        Double latitude = parseDouble(lineMatcher.group(5));
        String northOrSouth = lineMatcher.group(6);
        if ("S".equals(northOrSouth) && latitude != null)
            latitude = -latitude;
        Double longitude = parseDouble(lineMatcher.group(7));
        String westOrEasth = lineMatcher.group(8);
        if ("W".equals(westOrEasth) && longitude != null)
            longitude = -longitude;
        String height = lineMatcher.group(9);
        String speed = lineMatcher.group(10);
        String heading = lineMatcher.group(11);
        boolean isTypeA = trim(lineMatcher.group(12)) != null;
        String pressure = lineMatcher.group(13);
        String temperature = lineMatcher.group(14);
        String description = parseDescription(removeZeros(lineMatcher.group(15)), removeZeros(lineMatcher.group(1)), waypointType);

        CompactCalendar dateAndTime = parseDateAndTime(date, time);
        if(getUseLocalTimeZone())
            dateAndTime = dateAndTime.asUTCTimeInTimeZone(TimeZone.getTimeZone(getTimeZone()));
        Wgs84Position position = new Wgs84Position(longitude, latitude, parseDouble(height), parseDouble(speed),
                dateAndTime, description,
                context.getFile() != null && isTypeA ? new File(context.getFile().getParentFile(), description) : null);
        position.setWaypointType(waypointType);
        position.setHeading(parseDouble(heading));
        position.setPressure(parseDouble(pressure));
        position.setTemperature(parseDouble(temperature));
        return position;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String date = fillWithZeros(formatDate(position.getTime()), 6);
        String time = fillWithZeros(formatTime(position.getTime()), 6);
        String latitude = formatDoubleAsString(abs(position.getLatitude()), 6);
        String northOrSouth = position.getLatitude() != null && position.getLatitude() < 0.0 ? "S" : "N";
        String longitude = formatDoubleAsString(abs(position.getLongitude()), 6);
        String westOrEast = position.getLongitude() != null && position.getLongitude() < 0.0 ? "W" : "E";
        String height = fillWithZeros(position.getElevation() != null ? formatIntAsString(position.getElevation().intValue()) : "0", 5);
        String speed = position.getSpeed() != null ? formatDoubleAsString(position.getSpeed()) : "0";
        String heading = fillWithZeros(position.getHeading() != null ? formatIntAsString(position.getHeading().intValue()) : "0", 3);
        String pressure = position.getPressure() != null ? formatDoubleAsString(position.getPressure()) : "0";
        String temperature = fillWithZeros(position.getTemperature() != null ? formatIntAsString(position.getTemperature().intValue()) : "0", 2);
        String description = !isPositionDescription(position.getDescription()) ? position.getDescription() : "";

        writer.println(fillWithZeros(Integer.toString(index + 1), 6) + SEPARATOR +
                formatTag(position) + SEPARATOR +
                date + SEPARATOR + time + SEPARATOR +
                latitude + northOrSouth + SEPARATOR +
                longitude + westOrEast + SEPARATOR +
                height + SEPARATOR +
                speed + SEPARATOR +
                heading + SEPARATOR +
                pressure + SEPARATOR +
                temperature + SEPARATOR +
                fillWithZeros(escape(description, SEPARATOR, ';'), 8));
    }
}