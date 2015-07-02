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
package slash.navigation.routes;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Represents a route in the {@link Catalog}.
 *
 * @author Christian Pesch
 */

public interface Category {
    String getHref();
    String getName() throws IOException;

    List<Category> getCategories() throws IOException;
    Category create(String name) throws IOException;
    void update(Category parent, String name) throws IOException;
    void delete() throws IOException;

    List<Route> getRoutes() throws IOException;
    Route createRoute(String description, File localFile) throws IOException;
    Route createRoute(String description, String remoteUrl) throws IOException;
}
