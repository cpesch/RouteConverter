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
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleLineBasedFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.parseInteger;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatAccuracyAsString;
import static slash.navigation.common.NavigationConversion.formatElevationAsString;
import static slash.navigation.common.NavigationConversion.formatSpeedAsString;

/**
 * Reads and writes Qstarz BT-Q1000 (.csv) files.
 *
 * Header: INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HDOP,NSAT (USED/VIEW),DISTANCE,
 * Format: 8,T,2010/12/28,23:01:43,SPS,49.126389,N,8.614000,E,245.512 m,0.759 km/h,1.4,8(10),0.22 m,
 *
 * @author Christian Pesch
 */

public class QstarzQ1000Format extends SimpleLineBasedFormat<SimpleRoute> {
    protected static final Logger log = Logger.getLogger(QstarzQ1000Format.class.getName());

    private static final String HEADER_LINE = "INDEX,RCR,DATE,TIME,VALID,LATITUDE,N/S,LONGITUDE,E/W,HEIGHT,SPEED,HDOP,NSAT (USED/VIEW),DISTANCE,";
    private static final char SEPARATOR = ',';
    private static final String SPACE = "\\s*";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE + "(\\d+)" + SPACE + SEPARATOR +
                    SPACE + "([T])" + SPACE + SEPARATOR +
                    SPACE + "(\\d{4}/\\d{2}/\\d{2})?" + SPACE + SEPARATOR +
                    SPACE + "(\\d{2}:\\d{2}:\\d{2})?" + SPACE + SEPARATOR +
                    SPACE + "(.+)" + SPACE + SEPARATOR +

                    SPACE + "([\\d\\.]+)" + SPACE + SEPARATOR +
                    SPACE + "([NS])" + SPACE + SEPARATOR +
                    SPACE + "([\\d\\.]+)" + SPACE + SEPARATOR +
                    SPACE + "([WE])" + SPACE + SEPARATOR +

                    SPACE + "(" + POSITION + ")" + "[^" + SEPARATOR + "]*" + SEPARATOR +
                    SPACE + "([\\d\\.]+)" + "[^" + SEPARATOR + "]*" + SEPARATOR +
                    SPACE + "([\\d\\.]+)" + SPACE + SEPARATOR +
                    SPACE + "(\\d+)\\((\\d+)\\)" + SPACE + SEPARATOR +
                    SPACE + "([\\d\\.]+)" + "[^" + SEPARATOR + "]*" + SEPARATOR +
                    END_OF_LINE);

    private static final String DATE_AND_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy/MM/dd";
    private static final String TIME_FORMAT = "HH:mm:ss";

    public String getExtension() {
        return ".csv";
    }

    public String getName() {
        return "Qstarz BT-Q1000 (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }

    protected boolean isValidLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches() || line.startsWith(HEADER_LINE);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if(!matcher.matches())
            return false;
        String fix = matcher.group(5);
        return "SPS".equals(fix);
     }

    private CompactCalendar parseDateAndTime(String date, String time) {
        date = trim(date);
        time = trim(time);
        if(date == null || time == null)
            return null;
        String dateAndTime = date + " " + time;
        return parseDate(dateAndTime, DATE_AND_TIME_FORMAT);
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String date = lineMatcher.group(3);
        String time = lineMatcher.group(4);
        Double latitude = parseDouble(lineMatcher.group(6));
        String northOrSouth = lineMatcher.group(7);
        if ("S".equals(northOrSouth) && latitude != null)
            latitude = -latitude;
        Double longitude = parseDouble(lineMatcher.group(8));
        String westOrEasth = lineMatcher.group(9);
        if ("W".equals(westOrEasth) && longitude != null)
            longitude = -longitude;
        String height = lineMatcher.group(10);
        String speed = lineMatcher.group(11);
        String hdop = lineMatcher.group(12);
        String satellites = lineMatcher.group(13);

        Wgs84Position position = new Wgs84Position(longitude, latitude, parseDouble(height), parseDouble(speed),
                parseDateAndTime(date, time), null);
        position.setHdop(parseDouble(hdop));
        position.setSatellites(parseInteger(satellites));
        return position;
    }

    protected void writeHeader(PrintWriter writer, SimpleRoute route) {
        writer.println(HEADER_LINE);
    }

    private String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return createDateFormat(TIME_FORMAT).format(time.getTime());
    }

    private String formatDate(CompactCalendar date) {
        if (date == null)
            return "";
        return createDateFormat(DATE_FORMAT).format(date.getTime());
    }

    private Wgs84Position previousPosition;

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String date = formatDate(position.getTime());
        String time = formatTime(position.getTime());
        String latitude = formatDoubleAsString(abs(position.getLatitude()), 6);
        String northOrSouth = position.getLatitude() != null && position.getLatitude() < 0.0 ? "S" : "N";
        String longitude = formatDoubleAsString(abs(position.getLongitude()), 6);
        String westOrEast = position.getLongitude() != null && position.getLongitude() < 0.0 ? "W" : "E";
        String height = position.getElevation() != null ? formatElevationAsString(position.getElevation()) : "0.0";
        String speed = position.getSpeed() != null ? formatSpeedAsString(position.getSpeed()) : "0.0";
        String hdop = position.getHdop() != null ? formatAccuracyAsString(position.getHdop()) : "0.0";
        String satellites = position.getSatellites() != null ? formatIntAsString(position.getSatellites()) : "0";

        if (firstPosition)
            previousPosition = null;
        String distance = previousPosition != null ? formatElevationAsString(position.calculateDistance(previousPosition)) : "0.0";
        previousPosition = position;

        writer.println(Integer.toString(index + 1) + SEPARATOR + "T" + SEPARATOR +
                date + SEPARATOR + time + SEPARATOR + "SPS" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR +
                longitude + SEPARATOR + westOrEast + SEPARATOR +
                height + " m" + SEPARATOR +
                speed + " km/h" + SEPARATOR +
                hdop + SEPARATOR +
                satellites + "(" + satellites + ")" + SEPARATOR +
                distance + " m" + SEPARATOR);
    }
}