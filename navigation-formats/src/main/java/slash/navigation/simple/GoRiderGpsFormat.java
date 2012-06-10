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
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleLineBasedFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.escape;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Reads and writes GoRider GPS (.rt) files.
 * <p/>
 * Header: #CREATED="75" MODIFIED="76" NAME="Groningen - Noorderrondrit" <br/>
 * Format: STREET="Tjardaweg" PT="6.53616 53.24917"
 *
 * @author Christian Pesch
 */

public class GoRiderGpsFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final String HEADER = "#CREATED";
    private static final String STREET = "STREET";
    private static final String PT = "PT";
    private static final char NAME_VALUE_SEPARATOR = '=';
    private static final char QUOTE = '"';
    private static final String SPACE = ".*";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    SPACE + "(" + STREET + NAME_VALUE_SEPARATOR + QUOTE + "([^" + QUOTE + "]*)" + QUOTE + ")" +
                    SPACE + "(" + PT + NAME_VALUE_SEPARATOR + QUOTE + "(" + POSITION + ")\\s+(" + POSITION + ")" + QUOTE + ")" + SPACE +
                    END_OF_LINE);

    public String getName() {
        return "GoRider GPS (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".rt";
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }

    protected boolean isValidLine(String line) {
        return line.startsWith(HEADER) || isPosition(line);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String comment = lineMatcher.group(2);
        Double longitude = parseDouble(lineMatcher.group(4));
        Double latitude = parseDouble(lineMatcher.group(5));
        return new Wgs84Position(longitude, latitude, null, null, null, comment);
    }

    protected void writeHeader(PrintWriter writer) {
        writer.println(HEADER + NAME_VALUE_SEPARATOR + QUOTE + "100" + QUOTE +
                " MODIFIED" + NAME_VALUE_SEPARATOR + QUOTE + "100" + QUOTE +
                " NAME" + NAME_VALUE_SEPARATOR + QUOTE + "TODO" + QUOTE);
    }

    private static String formatComment(String string) {
        return escape(string, QUOTE, ';');
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String latitude = formatDoubleAsString(position.getLatitude(), 5);
        String longitude = formatDoubleAsString(position.getLongitude(), 5);
        String comment = formatComment(position.getComment());
        writer.println(STREET + NAME_VALUE_SEPARATOR + QUOTE + comment + QUOTE + " " +
                PT + NAME_VALUE_SEPARATOR + QUOTE + longitude + " " + latitude + QUOTE);
    }
}