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
package slash.navigation.converter.gui.helpers;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.routes.impl.RouteModel;

import static slash.common.io.Transfer.decodeUri;

/**
 * A helper for rendering aspects of {@link RouteModel}.
 *
 * @author Christian Pesch
 */

public class RouteHelper {
    public static String formatName(RouteModel route) {
        String name = route.getName();
        if (name == null)
            name = RouteConverter.getBundle().getString("no-name");
        return name;
    }

    public static String formatDescription(RouteModel route) {
        String description = "";
        try {
            description = route.getRoute().getDescription();
            if(description == null)
                formatName(route);
        } catch (Exception e) {
            // intentionally left empty
        }
        return description;
    }

    public static String formatCreator(RouteModel route) {
        String creator;
        try {
            creator = route.getRoute().getCreator();
            if (creator == null)
                creator = RouteConverter.getBundle().getString("no-creator");
        } catch (Exception e) {
            creator = RouteConverter.getBundle().getString("loading");
        }
        return creator;
    }

    public static String formatUrl(RouteModel route) {
        String url = route.getUrl();
        url = url.replaceAll("file:/", "");
        try {
            url = decodeUri(url);
        }
        catch (IllegalArgumentException e) {
            // intentionally left empty
        }
        return url;
    }
}
