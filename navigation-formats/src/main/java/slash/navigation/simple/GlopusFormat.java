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


/**
 * Reads and writes Glopus (.tk) files.
 * <p/>
 * Format: 51.0450383,7.0508300,Comment
 * 51.04503,7.05083
 *
 * @author Christian Pesch
 */

public class GlopusFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR = ',';

    // special position format to avoid detection of GarminPoiDbFormat where longitude and latitude are swapped
    private static final String POSITION5 = "-?\\d+\\.\\d{5}";
    private static final String POSITION7 = "-?\\d+\\.\\d{7}";

    private static final Pattern SIMPLE_LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION5 + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION5 + ")" + WHITE_SPACE +
                    END_OF_LINE);

    private static final Pattern COMMENT_LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION7 + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "(" + POSITION7 + ")" + WHITE_SPACE + SEPARATOR +
                    WHITE_SPACE + "([^\"]*)" + WHITE_SPACE +
                    END_OF_LINE);

    public String getExtension() {
        return ".tk";
    }

    public String getName() {
        return "Glopus (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher commentMatcher = COMMENT_LINE_PATTERN.matcher(line);
        if (commentMatcher.matches())
            return true;
        Matcher simpleMatcher = SIMPLE_LINE_PATTERN.matcher(line);
        return simpleMatcher.matches();
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher commentMatcher = COMMENT_LINE_PATTERN.matcher(line);
        if (commentMatcher.matches()) {
            String latitude = commentMatcher.group(1);
            String longitude = commentMatcher.group(2);
            String description = commentMatcher.group(3);
            return new Wgs84Position(parseDouble(longitude), parseDouble(latitude), null, null, null, trim(description));
        }

        Matcher simpleMatcher = SIMPLE_LINE_PATTERN.matcher(line);
        if (simpleMatcher.matches()) {
            String latitude = simpleMatcher.group(1);
            String longitude = simpleMatcher.group(2);
            return new Wgs84Position(parseDouble(longitude), parseDouble(latitude), null, null, null, null);
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = formatDoubleAsString(position.getLongitude(), 7);
        String latitude = formatDoubleAsString(position.getLatitude(), 7);
        String description = escape(position.getDescription(), SEPARATOR, ';');
        description = escape(description, '\"', ';');
        writer.println(latitude + SEPARATOR + longitude + SEPARATOR + description);
    }
}