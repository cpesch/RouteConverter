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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatElevationAsString;

/**
 * Reads and writes groundtrack vom SondenMonitor (.txt) files.
 *
*  Header:    Trackpunkt Nord     Ost     Hoehe  Aufstiegs-/Abstieg m/sek Zeit
 * Format:    57         52.73846 9.88738 -0.416 0.04850                  17:01:17.780
 *
 * @author Christian Pesch
 */

public class GroundTrackFormat extends SimpleLineBasedFormat<SimpleRoute> {
    protected static final Logger log = Logger.getLogger(GroundTrackFormat.class.getName());

    private static final String SPACE = "\\s+";
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE + "(\\d+)" +
                    SPACE + "(" + POSITION + ")" +
                    SPACE + "(" + POSITION + ")" +
                    SPACE + "(" + POSITION + ")" +
                    SPACE + "[\\d\\.]+" +
                    SPACE + "(-?\\d+:-?\\d+:-?\\d+)\\.?(\\d*)" +
                    ".*" +
                    END_OF_LINE);

    private static final String TIME_FORMAT = "HH:mm:ss.SSS";

    public String getExtension() {
        return ".txt";
    }

    public String getName() {
        return "groundtrack vom SondenMonitor (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private DateFormat createTimeFormat() {
        DateFormat dateFormat = createDateFormat(TIME_FORMAT);
        dateFormat.setLenient(false);
        return dateFormat;
    }

    private CompactCalendar parseTime(String time, String milliseconds) {
        if (time == null)
            return null;
        String dateString = time + "." + (milliseconds != null ? milliseconds : "000");
        try {
            Date parsed = createTimeFormat().parse(dateString);
            return fromDate(parsed);
        } catch (ParseException e) {
            log.severe("Could not parse time '" + dateString + "'");
        }
        return null;
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String description = trim(lineMatcher.group(1));
        Double latitude = parseDouble(lineMatcher.group(2));
        Double longitude = parseDouble(lineMatcher.group(3));
        Double elevation = parseDouble(lineMatcher.group(4));
        CompactCalendar time = parseTime(trim(lineMatcher.group(5)), trim(lineMatcher.group(6)));

        Wgs84Position position = new Wgs84Position(longitude, latitude, elevation, null, time, description);
        position.setStartDate(context.getStartDate());
        return position;
    }

    private String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return createTimeFormat().format(time.getTime());
    }

    private String fillWithSpaces(String string, int length) {
        StringBuilder buffer = new StringBuilder(string != null ? string : "");
        while (buffer.length() < length) {
            buffer.insert(0, ' ');
        }
        return buffer.toString();
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String latitude = formatDoubleAsString(position.getLatitude(), 6);
        String longitude = formatDoubleAsString(position.getLongitude(), 6);
        String elevation = position.getElevation() != null ? formatElevationAsString(position.getElevation()) : "0.0";
        String time = formatTime(position.getTime());

        // try to parse number from description to make read/write round trip easier
        int number;
        try {
            number = parseInt(position.getDescription());
        }
        catch (NumberFormatException e) {
            number = index + 1;
        }

        writer.println(fillWithSpaces(Integer.toString(number), 5) +
                fillWithSpaces(latitude, 12) +
                fillWithSpaces(longitude, 13) +
                fillWithSpaces(elevation, 11) +
                fillWithSpaces("0.0", 13) +
                " " + time);
    }
}