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
package slash.navigation.base;

import slash.navigation.babel.BabelFormat;
import slash.navigation.common.NavigationPosition;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.trim;

/**
 * Converts between different {@link NavigationFormat}.
 *
 * @author Christian Pesch
 */

public class NavigationFormatConverter {
    private static String removeDigits(String string) {
        StringBuilder buffer = new StringBuilder(string);
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (Character.isDigit(c)) {
                buffer.deleteCharAt(i);
                i--;
            }
        }
        return buffer.toString();
    }

    private static String getFormatName(NavigationFormat format) {
        Class<? extends NavigationFormat> formatClass = format.getClass();
        String formatName = formatClass.getSimpleName();
        if (trim(formatName) == null && formatClass.getSuperclass() != null)
            formatName = formatClass.getSuperclass().getSimpleName();
        // shortcut to prevent lots of as... methods
        if (format instanceof BabelFormat)
            formatName = "Gpx10Format";
        if (format instanceof GarbleNavigationFormat)
            formatName = formatName.substring("Garble".length());
        formatName = formatName.replaceAll("LittleEndian", "");
        return formatName;
    }

    /*package local for tests*/static BaseNavigationPosition asFormat(NavigationPosition position, NavigationFormat format) throws IOException {
        BaseNavigationPosition result;
        String formatName = getFormatName(format);
        formatName = formatName.replace("Format", "Position");
        formatName = removeDigits(formatName);
        try {
            Method method = position.getClass().getMethod("as" + formatName);
            result = (BaseNavigationPosition) method.invoke(position);
        } catch (Exception e) {
            throw new IOException("Cannot call as" + formatName + "() on " + position, e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static BaseRoute<BaseNavigationPosition, BaseNavigationFormat> asFormat(BaseRoute route, NavigationFormat format) throws IOException {
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> result;
        String formatName = getFormatName(format);
        try {
            Method method = route.getClass().getMethod("as" + formatName);
            result = (BaseRoute<BaseNavigationPosition, BaseNavigationFormat>) method.invoke(route);
        } catch (Exception e) {
            throw new IOException("Cannot call as" + formatName + "() on " + route, e);
        }
        return result;
    }

    public static List<BaseNavigationPosition> convertPositions(List<NavigationPosition> positions, NavigationFormat format) throws IOException {
        List<BaseNavigationPosition> result = new ArrayList<>(positions.size());
        for (NavigationPosition position : positions) {
            result.add(asFormat(position, format));
        }
        return result;
    }

    public static List<BaseRoute> convertRoute(List<BaseRoute> routes, NavigationFormat format) throws IOException {
        List<BaseRoute> result = new ArrayList<>(routes.size());
        for (BaseRoute route : routes) {
            result.add(asFormat(route, format));
        }
        return result;
    }
}
