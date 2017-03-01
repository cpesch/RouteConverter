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

package slash.navigation.nmn;

import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCalculations.asWgs84Position;

/**
 * Reads and writes Navigating POI-Warner (.asc) files.
 *
 * Format: 8.6180900,50.2175100,"[61352] AH Kreissl GmbH; Benzstrasse 7 [Bad Homburg]"
 *
 * @author Christian Pesch
 */

public class NavigatingPoiWarnerFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR = ',';

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "\"(.*)\"" + WHITE_SPACE +
                    END_OF_LINE);

    public String getExtension() {
        return ".asc";
    }

    public String getName() {
        return "Navigating POI-Warner (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || line.trim().startsWith(";");
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String longitude = lineMatcher.group(1);
        String latitude = lineMatcher.group(2);
        String description = trim(lineMatcher.group(3));
        if (description != null)
            description = description.replaceAll("\\p{Cntrl}", "");
        return asWgs84Position(parseDouble(longitude), parseDouble(latitude), description);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = formatDoubleAsString(position.getLongitude(), 7);
        String latitude = formatDoubleAsString(position.getLatitude(), 7);
        String description = escape(position.getDescription(), SEPARATOR, ';');
        writer.println(longitude + SEPARATOR + latitude + SEPARATOR + "\"" + description + "\"");
    }
}
