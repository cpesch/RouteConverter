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

package slash.navigation;

import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Glopus (.tk) files.
 *
 * Format: 51.0450383,7.0508300,
 *         51.0450383,7.0508300,50, 
 *
 * @author Christian Pesch
 */

public class GlopusFormat extends SimpleLineBasedFormat<SimpleRoute> {
    private static final char SEPARATOR_CHAR = ',';

    // special position format to avoid detection of GarminPoiDbFormat where longitude and latitude are swapped
    private static final String POSITION = "-?\\d+\\.\\d{7}";

    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "(" + POSITION + ")" + WHITE_SPACE + SEPARATOR_CHAR +
                    WHITE_SPACE + "([-\\d]*)" + WHITE_SPACE + SEPARATOR_CHAR + "?"+
                    WHITE_SPACE + "([^\"]*)" + WHITE_SPACE + SEPARATOR_CHAR + "?"+
                    END_OF_LINE);


    public String getExtension() {
        return ".tk";
    }

    public String getName() {
        return "Glopus (*" + getExtension() + ")";
    }
    
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected Wgs84Position parsePosition(String line, Calendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String latitude = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String elevation = lineMatcher.group(3);
        String comment = lineMatcher.group(4);
        return new Wgs84Position(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude),
                Conversion.parseDouble(elevation), null, null, Conversion.trim(comment));
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Conversion.formatDoubleAsString(position.getLongitude(), 7);
        String latitude = Conversion.formatDoubleAsString(position.getLatitude(), 7);
        String elevation = position.getElevation() != null ? Conversion.formatIntAsString(position.getElevation().intValue()) : "";
        String comment = position.getComment();
        writer.println(latitude + SEPARATOR_CHAR + longitude + SEPARATOR_CHAR + elevation + SEPARATOR_CHAR + comment);
    }
}