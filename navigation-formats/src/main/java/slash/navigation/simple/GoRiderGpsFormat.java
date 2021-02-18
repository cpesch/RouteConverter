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

import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * Reads and writes GoRider GPS (.rt) files.
 *
 * Header: #CREATED="75" MODIFIED="76" NAME="Groningen - Noorderrondrit"
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

    public String getExtension() {
        return ".rt";
    }

    public String getName() {
        return "GoRider GPS (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Route;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || line.startsWith(HEADER);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String description = lineMatcher.group(2);
        Double longitude = parseDouble(lineMatcher.group(4));
        Double latitude = parseDouble(lineMatcher.group(5));
        return asWgs84Position(longitude, latitude, description);
    }

    protected void writeHeader(PrintWriter writer, SimpleRoute route) {
        writer.println(HEADER + NAME_VALUE_SEPARATOR + QUOTE + "100" + QUOTE +
                " MODIFIED" + NAME_VALUE_SEPARATOR + QUOTE + "100" + QUOTE +
                " NAME" + NAME_VALUE_SEPARATOR + QUOTE + route.getName() + QUOTE);
    }

    private static String formatDescription(String string) {
        return escape(string, QUOTE, ';').replaceAll("<", " ").replaceAll(">", " ");
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String latitude = formatDoubleAsString(position.getLatitude(), 5);
        String longitude = formatDoubleAsString(position.getLongitude(), 5);
        String description = formatDescription(position.getDescription());
        writer.println(STREET + NAME_VALUE_SEPARATOR + QUOTE + description + QUOTE + " " +
                PT + NAME_VALUE_SEPARATOR + QUOTE + longitude + " " + latitude + QUOTE);
    }
}
