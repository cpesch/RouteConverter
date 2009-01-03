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

import slash.navigation.Wgs84Position;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Navigon Mobile Navigator 4 (.rte) files.
 *
 * @author Christian Pesch
 */

public class Nmn4Format extends NmnFormat {
    // -|-|-|-|-|-|-|-|-|-|-|6.42323|51.84617|-|-|
    // -|-|16|-|-|Linau|-|-|-|-|-|10.46348|53.64352|-|-|
    // -|-|-|-|-|-|-|-|-|-|7.00905|51.44329|-|
    // -|-|-|45128|Südviertel|45128|Hohenzollernstrasse/L451|-|-|-|7.00905|51.44329|-|
    private static final Pattern LINE_PATTERN = Pattern.
            compile(WILDCARD + SEPARATOR + WILDCARD + SEPARATOR + WILDCARD + SEPARATOR +
                    "(" + WILDCARD + ")" + SEPARATOR +
                    "(" + WILDCARD + ")" + SEPARATOR +
                    "(" + WILDCARD + ")" + SEPARATOR +
                    "(" + WILDCARD + ")" + SEPARATOR +
                    WILDCARD + SEPARATOR +
                    WILDCARD + SEPARATOR +
                    WILDCARD + SEPARATOR +
                    "(" + WILDCARD + SEPARATOR + ")?" +
                    "(" + POSITION + ")" + SEPARATOR + "(" + POSITION + ")" + SEPARATOR +
                    WILDCARD + SEPARATOR +
                    "(" + WILDCARD + SEPARATOR + ")?"
            );

    public String getName() {
        return "Navigon Mobile Navigator 4 (*" + getExtension() + ")";
    }


    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private static String parseForNmn4(String string) {
        String result = Conversion.trim(string);
        if (result != null && "-".equals(result))
            result = null;
        // this was currently only in NMN5, try it out for NMN4, too
        if (result != null && result.length() > 2 && result.toUpperCase().equals(result))
            result = Conversion.toMixedCase(result);
        return result;
    }

    protected NmnPosition parsePosition(String line, Calendar startDate) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");

        String zip = parseForNmn4(lineMatcher.group(1));
        String city = parseForNmn4(lineMatcher.group(2));
        if (city == null)
            city = parseForNmn4(lineMatcher.group(3));
        String street = parseForNmn4(lineMatcher.group(4));
        String longitude = parseForNmn4(lineMatcher.group(6));
        String latitude = parseForNmn4(lineMatcher.group(7));
        return new NmnPosition(Conversion.parseDouble(longitude), Conversion.parseDouble(latitude), zip, city, street, null);
    }

    private static String formatForNmn4(String string) {
        return string != null ? escapeSeparator(string) : "-";
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        NmnPosition nmnPosition = (NmnPosition) position;
        String longitude = Conversion.formatDoubleAsString(nmnPosition.getLongitude());
        String latitude = Conversion.formatDoubleAsString(nmnPosition.getLatitude());
        String zip = formatForNmn4(nmnPosition.isUnstructured() ? null : nmnPosition.getZip());
        String city = formatForNmn4(nmnPosition.isUnstructured() ? nmnPosition.getComment() : nmnPosition.getCity());
        String street = formatForNmn4(nmnPosition.isUnstructured() ? null : nmnPosition.getStreet());
        writer.println("-" + SEPARATOR_CHAR + "-" + SEPARATOR_CHAR + "-" + SEPARATOR_CHAR +
                zip + SEPARATOR_CHAR +
                city + SEPARATOR_CHAR +
                zip + SEPARATOR_CHAR +
                street + SEPARATOR_CHAR +
                "-" + SEPARATOR_CHAR + "-" + SEPARATOR_CHAR + "-" + SEPARATOR_CHAR +
                longitude + SEPARATOR_CHAR + latitude + SEPARATOR_CHAR +
                "-" + SEPARATOR_CHAR);
    }
}