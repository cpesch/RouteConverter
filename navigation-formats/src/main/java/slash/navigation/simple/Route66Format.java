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

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.*;

import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Route 66 POI (.csv) files.
 * <p/>
 * Format: 11.107167,49.375783,"HOLSTEINBRUCH BEI WORZELDORF B - GC13VV5"
 *
 * @author Christian Pesch
 */

public class Route66Format extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR_CHAR = ',';

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "\"([A-Z\\s\\d-]*)\"" + WHITE_SPACE +
                    END_OF_LINE);


    public String getExtension() {
        return ".csv";
    }

    public String getName() {
        return "Route 66 POI (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String longitude = lineMatcher.group(1);
        String latitude = lineMatcher.group(2);
        String comment = Transfer.trim(lineMatcher.group(3));
        if (comment != null && comment.toUpperCase().equals(comment))
            comment = Transfer.toMixedCase(comment);
        return new Wgs84Position(Transfer.parseDouble(longitude), Transfer.parseDouble(latitude),
                null, null, null, comment);
    }

    private static String formatForRoute66(String string) {
        return string != null ? string.replaceAll("\\" + SEPARATOR_CHAR, "").toUpperCase() : "";
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Transfer.formatDoubleAsString(position.getLongitude(), 6);
        String latitude = Transfer.formatDoubleAsString(position.getLatitude(), 6);
        String comment = formatForRoute66(position.getComment());
        writer.println(longitude + SEPARATOR_CHAR + latitude + SEPARATOR_CHAR + "\"" + comment + "\"");
    }
}
