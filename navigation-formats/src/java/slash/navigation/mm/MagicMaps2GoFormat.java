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

package slash.navigation.mm;

import slash.navigation.*;
import slash.navigation.util.CompactCalendar;
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
 * Reads and writes Magic Maps 2 Go (.txt) files.
 * <p/>
 * Format: 52.4135141 13.3115464 40.8000000 31.05.09 07:05:58
 *
 * @author Christian Pesch
 */

public class MagicMaps2GoFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static Logger log = Logger.getLogger(MagicMaps2GoFormat.class.getName());
    private static final char SEPARATOR_CHAR = ' ';
    private static final DateFormat DATE_AND_TIME_FORMAT = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    "(" + POSITION + ")" + SEPARATOR_CHAR +
                    "(" + POSITION + ")" + SEPARATOR_CHAR +
                    "(" + POSITION + ")" + SEPARATOR_CHAR +
                    "(\\d\\d\\.\\d\\d\\.\\d\\d)" + SEPARATOR_CHAR +
                    "(\\d\\d\\:\\d\\d\\:\\d\\d)" +
                    END_OF_LINE);


    public String getExtension() {
        return ".txt";
    }

    public String getName() {
        return "Magic Maps 2 Go (*" + getExtension() + ")";
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Track;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private CompactCalendar parseDateAndTime(String date, String time) {
        time = Conversion.trim(time);
        date = Conversion.trim(date);
        String dateAndTime = date + " " + time;
        try {
            Date parsed = DATE_AND_TIME_FORMAT.parse(dateAndTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return CompactCalendar.fromCalendar(calendar);
        } catch (ParseException e) {
            log.severe("Could not parse date and time '" + dateAndTime + "'");
        }
        return null;
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String latitude = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String elevation = lineMatcher.group(3);
        String date = lineMatcher.group(4);
        String time = lineMatcher.group(5);
        return new Wgs84Position(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude),
                Conversion.parseDouble(elevation), null, parseDateAndTime(date, time), null);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String latitude = Conversion.formatDoubleAsString(position.getLatitude(), 7);
        String longitude = Conversion.formatDoubleAsString(position.getLongitude(), 7);
        String elevation = Conversion.formatDoubleAsString(position.getElevation(), 7);
        String dateAndTime = position.getTime() != null ? DATE_AND_TIME_FORMAT.format(position.getTime().getTime()) : "00.00.00 00:00:=00";
        writer.println(latitude + SEPARATOR_CHAR + longitude + SEPARATOR_CHAR + elevation + SEPARATOR_CHAR + dateAndTime);
    }
}