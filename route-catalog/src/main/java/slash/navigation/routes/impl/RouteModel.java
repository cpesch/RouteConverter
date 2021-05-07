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

import java.util.Objects;
import java.util.logging.Logger;

/**
 * A model that encapsulates a {@link Route}.
 *
 * @author Christian Pesch
 */

public class RouteModel {
    private static final Logger log = Logger.getLogger(RouteModel.class.getName());
    private final CategoryTreeNode category;
    private final Route route;

    public RouteModel(CategoryTreeNode category, Route route) {
        this.category = category;
        this.route = route;
    }

    public CategoryTreeNode getCategory() {
        return category;
    }

    public Route getRoute() {
        return route;
    }

    public String getName() {
        try {
            return getRoute().getName();
        } catch (Exception e) {
            log.severe("Cannot get name: " + e);
            return "?";
        }
    }

    public String getDescription() {
        try {
            return getRoute().getDescription();
        } catch (Exception e) {
            log.severe("Cannot get description: " + e);
            return "?";
        }
    }

    public String getUrl() {
        try {
            return getRoute().getUrl();
        } catch (Exception e) {
            log.severe("Cannot get URL: " + e);
            return "?";
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteModel that = (RouteModel) o;
        return Objects.equals(getCategory(), that.getCategory()) &&
                Objects.equals(getRoute(), that.getRoute());
    }

    public int hashCode() {
        return Objects.hash(getCategory(), getRoute());
    }
}
