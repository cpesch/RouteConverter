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

import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.*;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Reads and writes MagicMaps2Go (.txt) files.
 *
 * Format: 52.4135141 13.3115464 40.8000000 31.05.09 07:05:58
 *
 * @author Christian Pesch
 */

public class MagicMaps2GoFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final Logger log = Logger.getLogger(MagicMaps2GoFormat.class.getName());
    
    private static final char SEPARATOR = ' ';
    private static final String DATE_AND_TIME_FORMAT = "dd.MM.yy HH:mm:ss";
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    "(" + POSITION + ")" + SEPARATOR +
                    "(" + POSITION + ")" + SEPARATOR +
                    "(" + POSITION + ")" + SEPARATOR +
                    "(\\d\\d\\.\\d\\d\\.\\d\\d)" + SEPARATOR +
                    "(\\d\\d\\:\\d\\d\\:\\d\\d)" +
                    END_OF_LINE);

    public String getExtension() {
        return ".txt";
    }

    public String getName() {
        return "MagicMaps2Go (*" + getExtension() + ")";
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private CompactCalendar parseDateAndTime(String date, String time) {
        time = trim(time);
        date = trim(date);
        String dateAndTime = date + " " + time;
        return parseDate(dateAndTime, DATE_AND_TIME_FORMAT);
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String latitude = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String elevation = lineMatcher.group(3);
        String date = lineMatcher.group(4);
        String time = lineMatcher.group(5);
        return new Wgs84Position(parseDouble(longitude), parseDouble(latitude),
                parseDouble(elevation), null, parseDateAndTime(date, time), null);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String latitude = formatDoubleAsString(position.getLatitude(), 7);
        String longitude = formatDoubleAsString(position.getLongitude(), 7);
        String elevation = formatDoubleAsString(position.getElevation(), 7);
        String dateAndTime = position.hasTime() ? createDateFormat(DATE_AND_TIME_FORMAT).format(position.getTime().getTime()) : "00.00.00 00:00:=00";
        writer.println(latitude + SEPARATOR + longitude + SEPARATOR + elevation + SEPARATOR + dateAndTime);
    }
}