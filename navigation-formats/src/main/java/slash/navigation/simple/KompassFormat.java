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

import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.parseDouble;

/**
 * Reads and writes Kompass (.tk) files.
 *
 * Format: 51.0450383,7.0508300,50.2
 *
 * @author Christian Pesch
 */

public class KompassFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR = ',';

    // special position format to avoid detection of GarminPoiDbFormat where longitude and latitude are swapped
    private static final String POSITION = "-?\\d+\\.\\d{7}";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR + "?" +
                    WHITE_SPACE + "([-\\d\\.]*)" + WHITE_SPACE +
                    END_OF_LINE);

    public String getExtension() {
        return ".tk";
    }

    public String getName() {
        return "Kompass (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String latitude = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String elevation = lineMatcher.group(3);
        return new Wgs84Position(parseDouble(longitude), parseDouble(latitude), parseDouble(elevation), null, null, null);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = formatDoubleAsString(position.getLongitude(), 7);
        String latitude = formatDoubleAsString(position.getLatitude(), 7);
        String elevation = position.getElevation() != null ? formatDoubleAsString(position.getElevation(), 1) : "0.0";
        writer.println(latitude + SEPARATOR + longitude + SEPARATOR + elevation);
    }
}
