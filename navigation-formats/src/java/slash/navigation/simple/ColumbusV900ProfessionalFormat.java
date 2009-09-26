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

import slash.navigation.Wgs84Position;
import slash.navigation.util.CompactCalendar;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Columbus V900 Professional (.csv) files.
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX<br/>
 * Format: 8     ,T,090508,075646,48.174411N,016.284588E,-235 ,0   ,0  ,3D,SPS ,1.6  ,1.3  ,0.9  ,
 *
 * @author Christian Pesch
 */

public class ColumbusV900ProfessionalFormat extends ColumbusV900Format {
    private static final String HEADER = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([CTV])" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(\\d*)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(\\d*)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([NS])" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([WE])" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([-\\d]+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\s\u0000]+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +

                    SPACE_OR_ZERO + "[^" + SEPARATOR_CHAR + "]*" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "[^" + SEPARATOR_CHAR + "]*" + SPACE_OR_ZERO + SEPARATOR_CHAR +

                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\.]+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([^" + SEPARATOR_CHAR + "]*)" + SPACE_OR_ZERO +
                    END_OF_LINE);

    public String getName() {
        return "Columbus V900 Professional (*" + getExtension() + ")";
    }

    protected String getHeader() {
        return HEADER;
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
        Double latitude = Conversion.parseDouble(lineMatcher.group(5));
        String northOrSouth = lineMatcher.group(6);
        if ("S".equals(northOrSouth) && latitude != null)
            latitude = -latitude;
        Double longitude = Conversion.parseDouble(lineMatcher.group(7));
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
        int commentSeparatorIndex = comment.lastIndexOf(SEPARATOR_CHAR);
        if (commentSeparatorIndex != -1)
            comment = comment.substring(commentSeparatorIndex + 1);
        comment = Conversion.trim(comment);

        String lineType = Conversion.trim(lineMatcher.group(2));
        if (comment == null && POI_POSITION.equals(lineType)) {
            String lineNumber = lineMatcher.group(1);
            comment = "POI " + Conversion.trim(removeZeros(lineNumber));
        }

        Wgs84Position position = new Wgs84Position(longitude, latitude, Conversion.parseDouble(height), Conversion.parseDouble(speed),
                parseDateAndTime(date, time), comment);
        position.setHeading(Conversion.parseDouble(heading));
        position.setPdop(Conversion.parseDouble(pdop));
        position.setHdop(Conversion.parseDouble(hdop));
        position.setVdop(Conversion.parseDouble(vdop));
        return position;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String date = fillWithZeros(formatDate(position.getTime()), 6);
        String time = fillWithZeros(formatTime(position.getTime()), 6);
        String latitude = Conversion.formatDoubleAsString(Math.abs(position.getLatitude()), 6);
        String northOrSouth = position.getLatitude() != null && position.getLatitude() < 0.0 ? "S" : "N";
        String longitude = Conversion.formatDoubleAsString(Math.abs(position.getLongitude()), 6);
        String westOrEast = position.getLongitude() != null && position.getLongitude() < 0.0 ? "W" : "E";
        String height = fillWithZeros(position.getElevation() != null ? Conversion.formatIntAsString(position.getElevation().intValue()) : "0", 5);
        String speed = fillWithZeros(position.getSpeed() != null ? Conversion.formatIntAsString(position.getSpeed().intValue()) : "0", 4);
        String heading = fillWithZeros(position.getHeading() != null ? Conversion.formatIntAsString(position.getHeading().intValue()) : "0", 3);
        String pdop = fillWithZeros(position.getPdop() != null ? Conversion.formatAccuracyAsString(position.getPdop()) : "0.0", 5);
        String hdop = fillWithZeros(position.getHdop() != null ? Conversion.formatAccuracyAsString(position.getHdop()) : "0.0", 5);
        String vdop = fillWithZeros(position.getVdop() != null ? Conversion.formatAccuracyAsString(position.getVdop()) : "0.0", 5);
        String comment = fillWithZeros(position.getComment() != null ? position.getComment().replaceAll(",", ";") : "", 8);

        writer.println(fillWithZeros(Integer.toString(index + 1), 6) + SEPARATOR_CHAR +
                formatLineType(position.getComment()) + SEPARATOR_CHAR +
                date + SEPARATOR_CHAR + time + SEPARATOR_CHAR +
                latitude + northOrSouth + SEPARATOR_CHAR +
                longitude + westOrEast + SEPARATOR_CHAR +
                height + SEPARATOR_CHAR +
                speed + SEPARATOR_CHAR +
                heading + SEPARATOR_CHAR +

                "3D" + SEPARATOR_CHAR +
                "SPS" + SEPARATOR_CHAR +

                pdop + SEPARATOR_CHAR +
                hdop + SEPARATOR_CHAR +
                vdop + SEPARATOR_CHAR +
                comment);
    }
}