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

package slash.navigation.routes.impl;

import slash.navigation.routes.Route;

import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;

/**
 * Compares {@link Route}s by their name.
 *
 * @author Christian Pesch
 */

public class RouteComparator implements Comparator<Route> {
    private String getName(Route route) {
        try {
            return route.getDescription() + "/" + route.getName();
        } catch (IOException e) {
            return "?";
        }
    }

    public int compare(Route c1, Route c2) {
        return Collator.getInstance().compare(getName(c1), getName(c2));
    }
}