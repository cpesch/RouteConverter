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

import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Position;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;

/**
 * Reads and writes Navigon Mobile Navigator 6 (.rte) files.
 *
 * Format: [D 22081,Hamburg/Uhlenhorst,Finkenau,0,|][0][10]|||10.03200|53.56949
 *
 * @author Christian Pesch
 */

public class Nmn6Format extends NmnFormat {
    // [||][2]
    private static final Pattern LINE_PATTERN = Pattern.
            compile("\\" + LEFT_BRACE + ".*" + "\\" + RIGHT_BRACE +
                    "\\" + LEFT_BRACE + ".*" + RIGHT_BRACE);

    private static final Pattern POSITION_PATTERN = Pattern.
            compile("\\" + LEFT_BRACE + "(" + WILDCARD + ")" + REGEX_SEPARATOR + "{1,2}" + "\\" + RIGHT_BRACE +
                    "\\" + LEFT_BRACE + "\\d+\\" + RIGHT_BRACE +
                    "\\" + LEFT_BRACE + "\\d+\\" + RIGHT_BRACE +
                    REGEX_SEPARATOR + WILDCARD +
                    REGEX_SEPARATOR + WILDCARD +
                    REGEX_SEPARATOR +
                    "(" + POSITION + ")" + REGEX_SEPARATOR + "(" + POSITION + ")" + "(.*)");

    public String getName() {
        return "Navigon Mobile Navigator 6 (*" + getExtension() + ")";
    }

    protected boolean isValidLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches() || isPosition(line);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = POSITION_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected NmnPosition parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = POSITION_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String description = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String latitude = lineMatcher.group(3);
        return new NmnPosition(parseDouble(longitude), parseDouble(latitude), (Double)null, null, null, trim(description));
    }

    private static String escapeBraces(String string) {
        return string != null ? string.replaceAll("[\\" + LEFT_BRACE + "|" + REGEX_SEPARATOR + "|\\" + RIGHT_BRACE + "]", ";") : "";
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = formatPositionAsString(position.getLongitude());
        String latitude = formatPositionAsString(position.getLatitude());
        String description = escapeBraces(position.getDescription());
        writer.println(LEFT_BRACE + description + SEPARATOR + RIGHT_BRACE +
                LEFT_BRACE + "0" + RIGHT_BRACE + LEFT_BRACE + "10" + RIGHT_BRACE +
                SEPARATOR + SEPARATOR + SEPARATOR +
                longitude + SEPARATOR + latitude + SEPARATOR + SEPARATOR);
    }
}
