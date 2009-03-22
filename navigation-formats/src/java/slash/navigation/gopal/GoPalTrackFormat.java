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

import slash.navigation.*;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes GoPal Track (.trk) files.
 *
 * @author Christian Pesch
 */

public class GoPalTrackFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR_CHAR = ',';

    // fortlaufende Zeit, Uhrzeit (hhmmss); MEZ, Länge, Breite, Winkel Fahrtrichtung, Geschwindigkeit, Com Port GPS, HDOP, Anzahl der empfangenen Satelliten
    // 6661343, 180817, 8.016822, 52.345300, 10.78, 38.1142, 2, 3.000000, 3
    // 6651145, 180807, 0.000000, 0.000000,      0,       0, 0, 0.000000, 0
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + POSITION + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + POSITION + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + POSITION + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE +
                    END_OF_LINE);


    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "GoPal Track (*" + getExtension() + ")";
    }

    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Track;
    }

    protected boolean shouldCreateRoute(List<Wgs84Position> positions) {
        // otherwhise NMN4Format aka gpsbabel -i nmn4 tries to read gopal track files and complains forever
        return positions.size() > 0;
    }

    protected boolean isValidLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches())
            return false;
        Integer satellites = Conversion.parseInt(matcher.group(4));
        return satellites != null && satellites > 0;
    }

    private Calendar parseTime(String time, Calendar startDate) {
        time = Conversion.trim(time);
        if (time == null || time.length() != 6)
            return null;
        Calendar calendar = Calendar.getInstance();
        calendar.set(1970, 0, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (startDate != null) {
            calendar.set(Calendar.YEAR, startDate.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, startDate.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, startDate.get(Calendar.DAY_OF_MONTH));
        }
        Integer hour = Conversion.parseInt(time.substring(0, 2));
        if (hour != null)
            calendar.set(Calendar.HOUR_OF_DAY, hour);
        Integer minute = Conversion.parseInt(time.substring(2, 4));
        if (minute != null)
            calendar.set(Calendar.MINUTE, minute);
        Integer second = Conversion.parseInt(time.substring(4, 6));
        if (second != null)
            calendar.set(Calendar.SECOND, second);
        return calendar;
    }

    protected Wgs84Position parsePosition(String line, Calendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String time = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String latitude = lineMatcher.group(3);
        return new Wgs84Position(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude),
                null, null, parseTime(time, startDate), null);
    }

    private String formatNumber(int number) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(number);
        while (buffer.length() < 2)
            buffer.insert(0, "0");
        while (buffer.length() > 2)
            buffer.deleteCharAt(0);
        return buffer.toString();
    }

    private String formatTime(Calendar time) {
        if (time == null)
            return "000000";
        return formatNumber(time.get(Calendar.HOUR_OF_DAY)) +
                formatNumber(time.get(Calendar.MINUTE)) +
                formatNumber(time.get(Calendar.SECOND));
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Conversion.formatDoubleAsString(position.getLongitude());
        String latitude = Conversion.formatDoubleAsString(position.getLatitude());
        String time = formatTime(position.getTime());
        writer.println("0" + SEPARATOR_CHAR + time + SEPARATOR_CHAR +
                longitude + SEPARATOR_CHAR + latitude + SEPARATOR_CHAR +
                "0.0" + SEPARATOR_CHAR + "0.0" + SEPARATOR_CHAR +
                "1" + SEPARATOR_CHAR + "0.0" + SEPARATOR_CHAR + "1");
    }
}
