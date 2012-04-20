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

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a {@link ParserContext}.
 *
 * @author Christian Pesch
 */

public class ParserContextImpl<R> implements ParserContext<R> {
    private List<R> routes = new ArrayList<R>();

    public void addRoute(R route) {
        this.routes.add(route);
    }

    public void addRoutes(List<R> routes) {
        this.routes.addAll(routes);
    }

    public List<R> getRoutes() {
        return routes;
    }
}
