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
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * Reads Kienzle GPS (.txt) files.
 *
 * Head: Position;X;Y;Empf&auml;nger;Land;PLZ;Ort;Strasse;Hausnummer;Planankunft;Zusatzinfos
 * Format: 118;7.0591660000;50.7527770000;PHE II;;53117;Bonn;Christian-Lassen-Str.;9;17:02;
 *
 * @author Christian Pesch
 */

public class KienzleGpsFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR = ';';
    private static final String HEADER_LINE = "Position;X;Y";
    private static final String TIME_FORMAT = "HH:mm";
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "\\d+" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(\\d*)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(.*)" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(\\d+:\\d+)" + WHITE_SPACE + SEPARATOR +
                    ".*" +
                    END_OF_LINE);

    public String getExtension() {
        return ".txt";
    }

    public String getName() {
        return "Kienzle GPS (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Route;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || line.startsWith(HEADER_LINE);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private CompactCalendar parseTime(String time) {
        if (time == null)
            return null;
        return parseDate(time, TIME_FORMAT);
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String longitude = lineMatcher.group(1);
        String latitude = lineMatcher.group(2);
        String organization = trim(lineMatcher.group(3));
        String postalCode = trim(lineMatcher.group(5));
        String city = trim(lineMatcher.group(6));
        String street = trim(lineMatcher.group(7));
        String houseNo = trim(lineMatcher.group(8));
        String time = lineMatcher.group(9);
        String description = (organization != null ? organization + ": " : "") +
                (postalCode != null ? postalCode + " " : "") +
                (city != null ? city + ", " : "") +
                (street != null ? street + " " : "") +
                (houseNo != null ? houseNo : "");

        CompactCalendar calendar = parseTime(time);
        Wgs84Position position = new Wgs84Position(parseDouble(longitude), parseDouble(latitude),
                null, null, calendar, description);
        position.setStartDate(context.getStartDate());
        return position;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException();
    }
}
