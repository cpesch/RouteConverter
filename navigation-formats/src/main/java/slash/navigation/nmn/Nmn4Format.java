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

import static slash.common.io.Transfer.*;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;

/**
 * Reads and writes Navigon Mobile Navigator 4 (.rte) files.
 *
 * Format: -|-|-|-|-|-|-|-|-|-|-|6.42323|51.84617|-|-|
 *         -|-|16|-|-|Linau|-|-|-|-|-|10.46348|53.64352|-|-|
 *         -|-|-|-|-|-|-|-|-|-|7.00905|51.44329|-|
 *         -|-|-|45128|S&uuml;dviertel|45128|Hohenzollernstrasse/L451|-|-|-|7.00905|51.44329|-|
 *         -|-|17|-|-|Gelsenkirchen|45896|Polsumer Strasse|-|-|-|7.05143|51.59682|-|-|
 *
 * @author Christian Pesch
 */

public class Nmn4Format extends NmnFormat {

    private static final Pattern LINE_PATTERN = Pattern.
            compile(WILDCARD + REGEX_SEPARATOR +
                    WILDCARD + REGEX_SEPARATOR +
                    WILDCARD + REGEX_SEPARATOR +
                    "(" + WILDCARD + ")" + REGEX_SEPARATOR +
                    "(" + WILDCARD + ")" + REGEX_SEPARATOR +
                    "(" + WILDCARD + ")" + REGEX_SEPARATOR +
                    "(" + WILDCARD + ")" + REGEX_SEPARATOR +
                    "(" + WILDCARD + ")" + REGEX_SEPARATOR +
                    WILDCARD + REGEX_SEPARATOR +
                    WILDCARD + REGEX_SEPARATOR +
                    "(" + WILDCARD + REGEX_SEPARATOR + ")?" +
                    "(" + POSITION + ")" + REGEX_SEPARATOR + "(" + POSITION + ")" + REGEX_SEPARATOR +
                    WILDCARD + REGEX_SEPARATOR +
                    "(" + WILDCARD + REGEX_SEPARATOR + ")?"
            );

    public String getName() {
        return "Navigon Mobile Navigator 4 (*" + getExtension() + ")";
    }

    protected boolean isPosition(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        return matcher.matches();
    }

    private static String parseForNmn4(String string) {
        String result = trim(string);
        if ("-".equals(result))
            result = null;
        // this was currently only in NMN5, try it out for NMN4, too
        if (result != null && result.length() > 2)
            result = toMixedCase(result);
        return result;
    }

    protected NmnPosition parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");

        String zip = parseForNmn4(lineMatcher.group(1));
        String city = parseForNmn4(lineMatcher.group(2));
        String street = parseForNmn4(lineMatcher.group(4));
        if (zip == null)
            zip = parseForNmn4(lineMatcher.group(4));
        if (city == null)
            city = parseForNmn4(lineMatcher.group(3));
        if (street != null && street.equals(zip))
            street = parseForNmn4(lineMatcher.group(5));
        String longitude = parseForNmn4(lineMatcher.group(7));
        String latitude = parseForNmn4(lineMatcher.group(8));
        return new NmnPosition(parseDouble(longitude), parseDouble(latitude), zip, city, street, null);
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        NmnPosition nmnPosition = (NmnPosition) position;
        String longitude = formatPositionAsString(nmnPosition.getLongitude());
        String latitude = formatPositionAsString(nmnPosition.getLatitude());
        String zip = escape(nmnPosition.isUnstructured() ? null : nmnPosition.getZip());
        String city = escape(nmnPosition.isUnstructured() ? nmnPosition.getDescription() : nmnPosition.getCity());
        String street = escape(nmnPosition.isUnstructured() ? null : nmnPosition.getStreet() + (nmnPosition.getNumber() != null ? " " + nmnPosition.getNumber() : ""));
        writer.println(
                "-" + SEPARATOR +
                "-" + SEPARATOR +
                "-" + SEPARATOR +
                "-" + SEPARATOR +
                "-" + SEPARATOR +
                city + SEPARATOR +
                zip + SEPARATOR +
                street + SEPARATOR +
                "-" + SEPARATOR +
                "-" + SEPARATOR +
                "-" + SEPARATOR +
                longitude + SEPARATOR +
                latitude + SEPARATOR +
                "-" + SEPARATOR +
                "-" + SEPARATOR
        );
    }
}