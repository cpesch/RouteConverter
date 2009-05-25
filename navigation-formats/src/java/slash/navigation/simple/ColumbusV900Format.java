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

import slash.navigation.*;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Columbus V900 (.csv) files.
 * 
 * Standard Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX<br/>
 * Standard Format: 1     ,T,090421,061051,47.797120N,013.049595E,524  ,33  ,0  ,<br/>
 * Professional Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX<br/>
 * Professional Format: 8     ,T,090508,075646,48.174411N,016.284588E,-235 ,0   ,0  ,3D,SPS ,1.6  ,1.3  ,0.9  ,
 *
 * @author Christian Pesch
 */

public class ColumbusV900Format extends SimpleLineBasedFormat<SimpleRoute> {
    private static Logger log = Logger.getLogger(ColumbusV900Format.class.getName());
    private static final String STANDARD_HEADER_LINE = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,VOX";
    private static final String PROFESSIONAL_HEADER_LINE = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,FIX MODE,VALID,PDOP,HDOP,VDOP,VOX";
    private static final char SEPARATOR_CHAR = ',';
    private static final String SPACE_OR_ZERO = "[\\s\u0000]*";
    private static final DateFormat DATE_AND_TIME_FORMAT = new SimpleDateFormat("yyMMdd HHmmss");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMdd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");
    private static final String WAYPOINT_POSITION = "T";
    private static final String VOICE_POSITION = "V";
    private static final String POI_POSITION = "C";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([CTV])" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(\\d{6})" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(\\d{6})" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([NS])" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([\\d\\.]+)([WE])" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "([-\\d]+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(\\d+)" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "\\d+" + SPACE_OR_ZERO + SEPARATOR_CHAR +
                    SPACE_OR_ZERO + "(.*)" + SPACE_OR_ZERO +
                    END_OF_LINE);

    public String getExtension() {
        return ".csv";
    }

    public String getName() {
        return "Columbus V900 (*" + getExtension() + ")";
    }

    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Track;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || line != null && (line.startsWith(STANDARD_HEADER_LINE) || line.startsWith(PROFESSIONAL_HEADER_LINE));
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private Calendar parseDateAndTime(String date, String time) {
        time = Conversion.trim(time);
        date = Conversion.trim(date);
        String dateAndTime = date + " " + time;
        try {
            Date parsed = DATE_AND_TIME_FORMAT.parse(dateAndTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException e) {
            log.severe("Could not parse date and time '" + dateAndTime + "'");
        }
        return null;
    }

    private String removeZeros(String string) {
        return string != null ? string.replace('\u0000', ' ') : "";
    }

    protected Wgs84Position parsePosition(String line, Calendar startDate) {
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
        String speed = lineMatcher.group(10);

        String comment = removeZeros(lineMatcher.group(11));
        int commentSeparatorIndex = comment.lastIndexOf(SEPARATOR_CHAR);
        if (commentSeparatorIndex != -1)
            comment = comment.substring(commentSeparatorIndex + 1);
        comment = Conversion.trim(comment);

        String lineType = Conversion.trim(lineMatcher.group(2));
        if (comment == null && POI_POSITION.equals(lineType)) {
            String lineNumber = lineMatcher.group(1);
            comment = "POI " + Conversion.trim(removeZeros(lineNumber));
        }
        return new Wgs84Position(longitude, latitude, Conversion.parseDouble(height), Conversion.parseDouble(speed),
                parseDateAndTime(date, time), comment);
    }


    protected void writeHeader(PrintWriter writer) {
        writer.println(STANDARD_HEADER_LINE);
    }

    private String fillWithZeros(String string, int length) {
        StringBuffer buffer = new StringBuffer(string != null ? string : "");
        while (buffer.length() < length) {
            buffer.append('\u0000');
        }
        return buffer.toString();
    }

    private String formatDate(Calendar date) {
        if (date == null)
            return "";
        return DATE_FORMAT.format(date.getTime());
    }

    private String formatTime(Calendar time) {
        if (time == null)
            return "";
        return TIME_FORMAT.format(time.getTime());
    }

    private String formatLineType(String comment) {
        if (comment != null) {
            if (comment.startsWith("VOX"))
                return VOICE_POSITION;
            if (comment.startsWith("POI")) {
                return POI_POSITION;
            }
        }
        return WAYPOINT_POSITION;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String date = formatDate(position.getTime());
        String time = formatTime(position.getTime());
        String latitude = Conversion.formatDoubleAsString(position.getLatitude(), 6);
        String northOrSouth = position.getLatitude() != null && position.getLatitude() < 0.0 ? "S" : "N";
        String longitude = Conversion.formatDoubleAsString(position.getLongitude(), 6);
        String westOrEast = position.getLongitude() != null && position.getLongitude() < 0.0 ? "W" : "E";
        String height = fillWithZeros(position.getElevation() != null ? Conversion.formatIntAsString(position.getElevation().intValue()) : "0", 5);
        String speed = fillWithZeros(position.getSpeed() != null ? Conversion.formatIntAsString(position.getSpeed().intValue()) : "0", 4);
        String comment = fillWithZeros(position.getComment() != null ? position.getComment() : "", 8);
        writer.println(fillWithZeros(Integer.toString(index + 1), 6) + SEPARATOR_CHAR +
                formatLineType(position.getComment()) + SEPARATOR_CHAR +
                date + SEPARATOR_CHAR + time + SEPARATOR_CHAR +
                latitude + northOrSouth + SEPARATOR_CHAR +
                longitude + westOrEast + SEPARATOR_CHAR +
                height + SEPARATOR_CHAR +
                speed + SEPARATOR_CHAR +
                fillWithZeros("0", 3) + SEPARATOR_CHAR +
                comment);
    }
}