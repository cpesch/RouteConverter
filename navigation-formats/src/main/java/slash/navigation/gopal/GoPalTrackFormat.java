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

package slash.navigation.gopal;

import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleLineBasedFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.parseInteger;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatAccuracyAsString;
import static slash.navigation.common.NavigationConversion.formatHeadingAsString;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;
import static slash.navigation.common.NavigationConversion.formatSpeedAsString;

/**
 * Reads and writes GoPal Track (.trk) files.
 *
 * Header: fortlaufende Zeit, Uhrzeit (hhmmss); MEZ, L&auml;nge, Breite, Winkel Fahrtrichtung, Geschwindigkeit, Com Port GPS, HDOP, Anzahl der empfangenen Satelliten, [Datum (yyyymmdd), ?, ?]
 * Format: 6661343, 180817, 8.016822, 52.345300, 10.78, 38.1142, 2, 3.000000, 3
 *         6651145, 180807, 0.000000, 0.000000,      0,       0, 0, 0.000000, 0
 *         31653, 092258, -22.760357, 65.125717, 334.4, 20.7424, 2, 1.000000, 8, 20100719, 0, 14
 *
 * @author Christian Pesch
 */

public class GoPalTrackFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR = ',';
    private static final String DATE_AND_TIME_FORMAT = "yyyyMMdd HHmmss";
    private static final String TIME_FORMAT = "HHmmss";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE + SEPARATOR + "?" +
                    WHITE_SPACE + "(\\d*)" + WHITE_SPACE + SEPARATOR + "?" +
                    WHITE_SPACE + "\\d*" + WHITE_SPACE + SEPARATOR + "?" +
                    WHITE_SPACE + "[-\\d]*" + WHITE_SPACE + SEPARATOR + "?" +
                    END_OF_LINE);

    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "GoPal Track (*" + getExtension() + ")";
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
        return matcher.matches();
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches())
            return false;
        Integer satellites = parseInteger(matcher.group(7));
        return satellites != null && satellites > 0;
    }

    private CompactCalendar parseDateAndTime(String date, String time) {
        time = trim(time);
        date = trim(date);
        if (date == null)
            return parseDate(time, TIME_FORMAT);
        String dateAndTime = date + " " + time;
        return parseDate(dateAndTime, DATE_AND_TIME_FORMAT);
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String time = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String latitude = lineMatcher.group(3);
        String heading = lineMatcher.group(4);
        String speed = lineMatcher.group(5);
        String hdop = lineMatcher.group(6);
        String satellites = lineMatcher.group(7);
        String date = trim(lineMatcher.group(8));

        Wgs84Position position = new Wgs84Position(parseDouble(longitude), parseDouble(latitude),
                null, parseDouble(speed), parseDateAndTime(date, time), null);
        if (date == null)
            position.setStartDate(context.getStartDate());
        position.setHeading(parseDouble(heading));
        position.setHdop(parseDouble(hdop));
        position.setSatellites(parseInteger(satellites));
        return position;
    }

    private String formatNumber(int number) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(number);
        while (buffer.length() < 2)
            buffer.insert(0, "0");
        while (buffer.length() > 2)
            buffer.deleteCharAt(0);
        return buffer.toString();
    }

    private String formatTime(CompactCalendar time) {
        if (time == null)
            return "000000";
        Calendar calendar = time.getCalendar();
        return formatNumber(calendar.get(HOUR_OF_DAY)) +
               formatNumber(calendar.get(MINUTE)) +
               formatNumber(calendar.get(SECOND));
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = formatPositionAsString(position.getLongitude());
        String latitude = formatPositionAsString(position.getLatitude());
        String time = formatTime(position.getTime());
        String heading = formatHeadingAsString(position.getHeading());
        String speed = formatSpeedAsString(position.getSpeed());
        String hdop = formatAccuracyAsString(position.getHdop());
        Integer satellites = position.getSatellites();
        // since positions with zero satellites are ignored during reading
        if (satellites == null || satellites == 0)
            satellites = 1;
        String satellitesStr = formatIntAsString(satellites);
        writer.println("0" + SEPARATOR + " " +
                       time + SEPARATOR + " " +
                       longitude + SEPARATOR + " " +
                       latitude + SEPARATOR + " " +
                       heading + SEPARATOR + " " +
                       speed + SEPARATOR + " " +
                       "1" + SEPARATOR + " " +
                       hdop + SEPARATOR + " " +
                       satellitesStr);
    }
}
