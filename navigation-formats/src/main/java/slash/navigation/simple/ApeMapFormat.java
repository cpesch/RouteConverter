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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.parseLong;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Reads ape@map (.trk) files.
 *
 * [track]
 * (47.14454650878906,15.500686645507813)
 * (47.13923645019531,15.501678466796875)
 * --start--
 * (47.14453887939453,15.501603126525879,461.0,1370091148)
 * (47.14454650878906,15.501594543457031,503.4,1370091545)
 * (47.14454650878906,15.501594543457031,456.0,1370091547)
 * (47.144256591796875,15.501456260681152,449.0,1370091775)
 *
 * @author Martin Oberzalek
 */

public class ApeMapFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR = ',';
    private static final String FIRST_HEADER_LINE = "[track]";
    private static final String SECOND_HEADER_LINE = "--start--";
    private static final String TIME = "[0-9]+";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    "\\(" + "(" + POSITION + ")" + SEPARATOR + "(" + POSITION + ")" + SEPARATOR + "(" + POSITION + ")" + SEPARATOR + "(" + TIME + ")" + "\\)" +
                    "(;#CMDNewSegment)*" +
                    "\\s*" +
                    END_OF_LINE);

    private static final Pattern START_POSITION_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    "\\(" + "(" + POSITION + ")" + SEPARATOR + "(" + POSITION + ")" + "\\)" +
                    "\\s*" +
                    END_OF_LINE);

    public String getName() {
        return "ape@map (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".trk";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) ||
                line.startsWith(FIRST_HEADER_LINE) ||
                line.startsWith(SECOND_HEADER_LINE) ||
                isStartPosition(line);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private boolean isStartPosition(String line) {
        Matcher matcher = START_POSITION_PATTERN.matcher(line);
        return matcher.matches();
    }

    private CompactCalendar parseTime(String time) {
        Long milliseconds = parseLong(time);
        if (isEmpty(milliseconds))
            return null;
        return fromMillis(milliseconds * 1000);
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String latitude = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String altitude = lineMatcher.group(3);
        String time = lineMatcher.group(4);
        return new Wgs84Position(parseDouble(longitude), parseDouble(latitude),
                parseDouble(altitude), 0.0, parseTime(time), null);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException();
    }
}
