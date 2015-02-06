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
package slash.navigation.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;

/**
 * Provides {@link NavigationPosition} parsing from strings.
 *
 * @author Christian Pesch
 */

public class PositionParser {
    private static final String NUMBER = "[[-|+]|\\d|\\.|E]";
    private static final Pattern POSITION_PATTERN = Pattern.compile("(\\s*" + NUMBER + "*\\s*),(\\s*" + NUMBER + "*\\s*)(,\\s*" + NUMBER + "+\\s*)?\\s*");
    private static final String SEPARATOR = "[\\s|,]+";
    private static final Pattern EXTENSION_POSITION_PATTERN = Pattern.compile("\\s*(" + NUMBER + "+)" + SEPARATOR + "(" + NUMBER + "+)" + SEPARATOR + "(" + NUMBER + "+)\\s*");

    public static boolean isPosition(String coordinates) {
        Matcher matcher = POSITION_PATTERN.matcher(coordinates);
        return matcher.matches();
    }

    public static NavigationPosition parsePosition(String coordinates, String description) {
        Matcher matcher = POSITION_PATTERN.matcher(coordinates);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + coordinates + "' does not match");
        String longitude = matcher.group(1);
        String latitude = matcher.group(2);
        String elevation = matcher.group(3);
        if(elevation != null && elevation.startsWith(","))
            elevation = elevation.substring(1);
        return new SimpleNavigationPosition(parseDouble(longitude), parseDouble(latitude), parseDouble(elevation), trim(description));
    }

    public static List<NavigationPosition> parsePositions(String listOfCoordinates) {
        List<NavigationPosition> result = new ArrayList<>();
        Matcher matcher = POSITION_PATTERN.matcher(listOfCoordinates);
        while (matcher.find()) {
            result.add(parsePosition(matcher.group(0), null));
        }
        return result;
    }

    public static List<NavigationPosition> parseExtensionPositions(String listOfCoordinates) {
        List<NavigationPosition> result = new ArrayList<>();
        Matcher matcher = EXTENSION_POSITION_PATTERN.matcher(listOfCoordinates);
        while (matcher.find()) {
            String longitude = matcher.group(1);
            String latitude = matcher.group(2);
            String elevation = matcher.group(3);
            result.add(new SimpleNavigationPosition(parseDouble(longitude), parseDouble(latitude), parseDouble(elevation), null));
        }
        return result;
    }
}
