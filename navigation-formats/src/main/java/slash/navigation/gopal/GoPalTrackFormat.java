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

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.*;

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
 * Reads and writes GoPal Track (.trk) files.
 *
 * Header: fortlaufende Zeit, Uhrzeit (hhmmss); MEZ, L&auml;nge, Breite, Winkel Fahrtrichtung, Geschwindigkeit, Com Port GPS, HDOP, Anzahl der empfangenen Satelliten, [Datum (yyyymmdd), ?, ?]<br/>
 * Format: 6661343, 180817, 8.016822, 52.345300, 10.78, 38.1142, 2, 3.000000, 3<br/>
 *         6651145, 180807, 0.000000, 0.000000,      0,       0, 0, 0.000000, 0<br/>
 *         31653, 092258, -22.760357, 65.125717, 334.4, 20.7424, 2, 1.000000, 8, 20100719, 0, 14
 *
 * @author Christian Pesch
 */

public class GoPalTrackFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final Logger log = Logger.getLogger(GoPalTrackFormat.class.getName());

    private static final char SEPARATOR_CHAR = ',';
    private static final DateFormat DATE_AND_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd HHmmss");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");
    static {
        DATE_AND_TIME_FORMAT.setTimeZone(CompactCalendar.UTC);
        TIME_FORMAT.setTimeZone(CompactCalendar.UTC);
    }

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(\\d+)" + WHITE_SPACE + SEPARATOR_CHAR + "?" +
                    WHITE_SPACE + "(\\d*)" + WHITE_SPACE + SEPARATOR_CHAR + "?" +
                    WHITE_SPACE + "\\d*" + WHITE_SPACE + SEPARATOR_CHAR + "?" +
                    WHITE_SPACE + "[-\\d]*" + WHITE_SPACE + SEPARATOR_CHAR + "?" +
                    END_OF_LINE);


    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "GoPal Track (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
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
        Integer satellites = Transfer.parseInt(matcher.group(7));
        return satellites != null && satellites > 0;
    }

    private CompactCalendar parseTime(String time) {
        try {
            Date parsed = TIME_FORMAT.parse(time);
            return CompactCalendar.fromDate(parsed);
        } catch (ParseException e) {
            log.severe("Could not parse time '" + time + "'");
        }
        return null;
    }

    private CompactCalendar parseDateAndTime(String date, String time) {
        time = Transfer.trim(time);
        date = Transfer.trim(date);
        if (date == null)
            return parseTime(time);
        String dateAndTime = date + " " + time;
        try {
            Date parsed = DATE_AND_TIME_FORMAT.parse(dateAndTime);
            return CompactCalendar.fromDate(parsed);
        } catch (ParseException e) {
            log.severe("Could not parse date and time '" + dateAndTime + "'");
        }
        return null;
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
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
        String date = Transfer.trim(lineMatcher.group(8));

        Wgs84Position position = new Wgs84Position(Transfer.parseDouble(longitude), Transfer.parseDouble(latitude),
                null, Transfer.parseDouble(speed), parseDateAndTime(date, time), null);
        if (date == null)
            position.setStartDate(startDate);
        position.setHeading(Transfer.parseDouble(heading));
        position.setHdop(Transfer.parseDouble(hdop));
        position.setSatellites(Transfer.parseInt(satellites));
        return position;
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

    private String formatTime(CompactCalendar time) {
        if (time == null)
            return "000000";
        Calendar calendar = time.getCalendar();
        return formatNumber(calendar.get(Calendar.HOUR_OF_DAY)) +
               formatNumber(calendar.get(Calendar.MINUTE)) +
               formatNumber(calendar.get(Calendar.SECOND));
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Transfer.formatPositionAsString(position.getLongitude());
        String latitude = Transfer.formatPositionAsString(position.getLatitude());
        String time = formatTime(position.getTime());
        String heading = Transfer.formatHeadingAsString(position.getHeading());
        String speed = Transfer.formatSpeedAsString(position.getSpeed());
        String hdop = Transfer.formatAccuracyAsString(position.getHdop());
        Integer satellites = position.getSatellites();
        // since positions with zero satellites are ignored during reading
        if (satellites == null || satellites == 0)
            satellites = 1;
        String satellitesStr = Transfer.formatIntAsString(satellites);
        writer.println("0" + SEPARATOR_CHAR + " " +
                       time + SEPARATOR_CHAR + " " +
                       longitude + SEPARATOR_CHAR + " " +
                       latitude + SEPARATOR_CHAR + " " +
                       heading + SEPARATOR_CHAR + " " +
                       speed + SEPARATOR_CHAR + " " +
                       "1" + SEPARATOR_CHAR + " " +
                       hdop + SEPARATOR_CHAR + " " +
                       satellitesStr);
    }
}
