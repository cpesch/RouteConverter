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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.nmn;

import slash.navigation.Wgs84Position;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;

/**
 * Reads and writes Navigon Mobile Navigator 6 (.rte) files.
 *
 * @author Christian Pesch
 */

public class Nmn6Format extends NmnFormat {
    // [||][2]
    private static final Pattern LINE_PATTERN = Pattern.
            compile("\\" + LEFT_BRACE + ".*" + "\\" + RIGHT_BRACE +
                    "\\" + LEFT_BRACE + ".*" + RIGHT_BRACE);

    // [D 22081,Hamburg/Uhlenhorst,Finkenau,0,|][0][10]|||10.03200|53.56949
    private static final Pattern POSITION_PATTERN = Pattern.
            compile("\\" + LEFT_BRACE + "(" + WILDCARD + ")" + SEPARATOR + "{1,2}" + "\\" + RIGHT_BRACE +
                    "\\" + LEFT_BRACE + "\\d+\\" + RIGHT_BRACE +
                    "\\" + LEFT_BRACE + "\\d+\\" + RIGHT_BRACE +
                    SEPARATOR + WILDCARD +
                    SEPARATOR + WILDCARD +
                    SEPARATOR +
                    "(" + POSITION + ")" + SEPARATOR + "(" + POSITION + ")" + "(.*)");

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

    protected NmnPosition parsePosition(String line, Calendar startDate) {
        Matcher lineMatcher = POSITION_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        String comment = lineMatcher.group(1);
        String longitude = lineMatcher.group(2);
        String latitude = lineMatcher.group(3);
        return new NmnPosition(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude), (Double)null, null, Conversion.trim(comment));
    }

    private static String formatForNmn6(String string) {
        return string != null ? string.replaceAll("[\\" + LEFT_BRACE + "|" + SEPARATOR + "|\\" + RIGHT_BRACE + "]", ";") : "";
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Conversion.formatDoubleAsString(position.getLongitude());
        String latitude = Conversion.formatDoubleAsString(position.getLatitude());
        String comment = formatForNmn6(position.getComment());
        writer.println(LEFT_BRACE + comment + SEPARATOR_CHAR + RIGHT_BRACE +
                LEFT_BRACE + "0" + RIGHT_BRACE + LEFT_BRACE + "10" + RIGHT_BRACE +
                SEPARATOR_CHAR + SEPARATOR_CHAR + SEPARATOR_CHAR +
                longitude + SEPARATOR_CHAR + latitude + SEPARATOR_CHAR + SEPARATOR_CHAR);
    }
}
