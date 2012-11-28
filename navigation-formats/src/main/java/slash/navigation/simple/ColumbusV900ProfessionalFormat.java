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
import static slash.navigation.common.NavigationConversion.formatAccuracyAsString;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;

/**
 * Reads and writes Columbus V900/V990 Professional (.csv) files.
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX<br/>
 * Format: 8     ,T,090508,075646,48.174411N,016.284588E,-235 ,0   ,0  ,3D,SPS ,1.6  ,1.3  ,0.9  ,
 *
 * @author Christian Pesch
 */

public class ColumbusV900ProfessionalFormat extends ColumbusV900Format {
    private static final String HEADER_LINE = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX";

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

                    SPACE_OR_ZERO + "[^" + SEPARATOR + "]*" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "[^" + SEPARATOR + "]*" + SPACE_OR_ZERO + SEPARATOR +

                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR +
                    SPACE_OR_ZERO + "([^" + SEPARATOR + "]*)" + SPACE_OR_ZERO +
                    END_OF_LINE);

    public String getName() {
        return "Columbus V900 Professional (*" + getExtension() + ")";
    }

    protected String getHeader() {
        return HEADER_LINE;
    }

    protected Pattern getPattern() {
        return LINE_PATTERN;
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
        String pdop = lineMatcher.group(12);
        String hdop = lineMatcher.group(13);
        String vdop = lineMatcher.group(14);

        String comment = removeZeros(lineMatcher.group(15));
        int commentSeparatorIndex = comment.lastIndexOf(SEPARATOR);
        if (commentSeparatorIndex != -1)
            comment = comment.substring(commentSeparatorIndex + 1);
        comment = trim(comment);

        String lineType = trim(lineMatcher.group(2));
        if (comment == null && POI_POSITION.equals(lineType)) {
            String lineNumber = lineMatcher.group(1);
            comment = "POI " + trim(removeZeros(lineNumber));
        }

        Wgs84Position position = new Wgs84Position(longitude, latitude, parseDouble(height), parseDouble(speed),
                parseDateAndTime(date, time), comment);
        position.setHeading(parseDouble(heading));
        position.setPdop(parseDouble(pdop));
        position.setHdop(parseDouble(hdop));
        position.setVdop(parseDouble(vdop));
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
        String pdop = fillWithZeros(position.getPdop() != null ? formatAccuracyAsString(position.getPdop()) : "0.0", 5);
        String hdop = fillWithZeros(position.getHdop() != null ? formatAccuracyAsString(position.getHdop()) : "0.0", 5);
        String vdop = fillWithZeros(position.getVdop() != null ? formatAccuracyAsString(position.getVdop()) : "0.0", 5);
        String comment = fillWithZeros(escape(position.getComment(), SEPARATOR, ';'), 8);

        writer.println(fillWithZeros(Integer.toString(index + 1), 6) + SEPARATOR +
                formatLineType(position.getComment()) + SEPARATOR +
                date + SEPARATOR + time + SEPARATOR +
                latitude + northOrSouth + SEPARATOR +
                longitude + westOrEast + SEPARATOR +
                height + SEPARATOR +
                speed + SEPARATOR +
                heading + SEPARATOR +
                "3D" + SEPARATOR +
                "SPS" + SEPARATOR +
                pdop + SEPARATOR +
                hdop + SEPARATOR +
                vdop + SEPARATOR +
                comment);
    }
}