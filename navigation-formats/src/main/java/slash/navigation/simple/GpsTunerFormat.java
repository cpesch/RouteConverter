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

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.*;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes GPS Tuner (.trk) files.
 * <p/>
 * Header: Latitude(Degree);Longitude(Degree);Altitude(m);Speed(kmph);Date(Unix TimeStamp);Segment;Heading(Degree)<br/>
 * Format: 50.3965966666667;7.53247333333333;74.4000015258789;77.56176;1172932595;1;279
 *
 * @author Christian Pesch
 */

public class GpsTunerFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final String FIRST_HEADER_LINE = "GPS Tracklog - ";
    private static final String SECOND_HEADER_LINE = "Latitude(Degree);Longitude(Degree);Altitude(m);Speed(kmph);Date(Unix TimeStamp);Segment;Heading(Degree)";
    private static final char SEPARATOR_CHAR = ';';

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE +
                    END_OF_LINE);


    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "GPS Tuner (*" + getExtension() + ")";
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Route;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || line != null && (line.startsWith(FIRST_HEADER_LINE) || line.startsWith(SECOND_HEADER_LINE));
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private CompactCalendar parseTime(String time) {
        Long milliseconds = Transfer.parseLong(time);
        if (milliseconds == null || milliseconds == 0)
            return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds * 1000);
        return CompactCalendar.fromCalendar(calendar);
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String latitude = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String altitude = lineMatcher.group(3);
        String speed = lineMatcher.group(4);
        String time = lineMatcher.group(5);
        String heading = lineMatcher.group(6);
        Wgs84Position position = new Wgs84Position(Transfer.parseDouble(longitude), Transfer.parseDouble(latitude),
                Transfer.parseDouble(altitude), Transfer.parseDouble(speed), parseTime(time), null);
        position.setHeading(Transfer.parseDouble(heading));
        return position;
    }


    protected void writeHeader(PrintWriter writer) {
        writer.println(FIRST_HEADER_LINE + GENERATED_BY);
        writer.println(SECOND_HEADER_LINE);
    }

    private String formatTime(CompactCalendar time) {
        if (time == null)
            return "0";
        return Long.toString(time.getTimeInMillis() / 1000);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Transfer.formatPositionAsString(position.getLongitude());
        String latitude = Transfer.formatPositionAsString(position.getLatitude());
        String altitude = Transfer.formatElevationAsString(position.getElevation());
        String speed = Transfer.formatSpeedAsString(position.getSpeed());
        String time = formatTime(position.getTime());
        String heading = position.getHeading() != null ? Transfer.formatIntAsString(position.getHeading().intValue()) : "0";
        writer.println(latitude + SEPARATOR_CHAR + longitude + SEPARATOR_CHAR + altitude + SEPARATOR_CHAR +
                speed + SEPARATOR_CHAR + time + SEPARATOR_CHAR + (firstPosition ? "1" : "0") + SEPARATOR_CHAR + heading);
    }
}
