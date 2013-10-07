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

package slash.navigation.simple;

import slash.common.type.CompactCalendar;
import slash.navigation.base.Wgs84Position;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.escape;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;

/**
 * Reads and writes Columbus V900 Standard (.csv) files.
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX<br/>
 * Format: 1     ,T,090421,061051,47.797120N,013.049595E,524  ,33  ,0  ,
 *
 * @author Christian Pesch
 */

public class ColumbusV900StandardFormat extends ColumbusV900Format {
    private static final String HEADER_LINE = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX";
    private static final Pattern HEADER_PATTERN = Pattern.compile(HEADER_LINE);
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([CGTV])" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "(\\d*)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "(\\d*)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([NS])" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([WE])" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([-\\d]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\s\u0000]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([^" + SEPARATOR + "]*)" + SPACE_OR_ZERO +
                    END_OF_LINE);

    public String getName() {
        return "Columbus V900 Standard (*" + getExtension() + ")";
    }

    protected Pattern getLinePattern() {
        return LINE_PATTERN;
    }

    protected String getHeader() {
        return HEADER_LINE;
    }

    protected Pattern getHeaderPattern() {
        return HEADER_PATTERN;
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
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
        String speed = lineMatcher.group(10).replaceAll(SPACE_OR_ZERO, "");
        String heading = lineMatcher.group(11);

        String description = removeZeros(lineMatcher.group(12));
        int descriptionSeparatorIndex = description.lastIndexOf(SEPARATOR);
        if (descriptionSeparatorIndex != -1)
            description = description.substring(descriptionSeparatorIndex + 1);
        description = trim(description);

        String lineType = trim(lineMatcher.group(2));
        if (description == null && POI_POSITION.equals(lineType)) {
            String lineNumber = lineMatcher.group(1);
            description = "POI " + trim(removeZeros(lineNumber));
        }

        Wgs84Position position = new Wgs84Position(longitude, latitude, parseDouble(height), parseDouble(speed),
                parseDateAndTime(date, time), description);
        position.setHeading(parseDouble(heading));
        return position;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String date = fillWithZeros(formatDate(position.getTime()), 6);
        String time = fillWithZeros(formatTime(position.getTime()), 6);
        String latitude = formatDoubleAsString(Math.abs(position.getLatitude()), 6);
        String northOrSouth = position.getLatitude() != null && position.getLatitude() < 0.0 ? "S" : "N";
        String longitude = formatDoubleAsString(Math.abs(position.getLongitude()), 6);
        String westOrEast = position.getLongitude() != null && position.getLongitude() < 0.0 ? "W" : "E";
        String height = fillWithZeros(position.getElevation() != null ? formatIntAsString(position.getElevation().intValue()) : "0", 5);
        String speed = fillWithZeros(position.getSpeed() != null ? formatIntAsString(position.getSpeed().intValue()) : "0", 4);
        String heading = fillWithZeros(position.getHeading() != null ? formatIntAsString(position.getHeading().intValue()) : "0", 3);
        String description = fillWithZeros(escape(position.getDescription(), SEPARATOR, ';'), 8);

        writer.println(fillWithZeros(Integer.toString(index + 1), 6) + SEPARATOR +
                formatLineType(position.getDescription()) + SEPARATOR +
                date + SEPARATOR + time + SEPARATOR +
                latitude + northOrSouth + SEPARATOR +
                longitude + westOrEast + SEPARATOR +
                height + SEPARATOR +
                speed + SEPARATOR +
                heading + SEPARATOR +
                description);
    }
}