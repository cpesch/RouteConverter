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

package slash.navigation.converter.gui.services;

import slash.common.io.Files;

import java.io.File;
import java.io.IOException;

/**
 * The {@link RouteService} at http://www.routeconverter.com/catalog
 *
 * @author Christian Pesch
 */

public class RouteCatalog implements RouteService {
    // todo share with the one in BrowsePanel?
    private final slash.navigation.catalog.domain.RouteCatalog routeCatalog = new slash.navigation.catalog.domain.RouteCatalog(System.getProperty("catalog", "http://www.routeconverter.com/catalog/"));

    public String getName() {
        return "RouteCatalog";
    }

    public boolean isOriginOf(String url) {
        return url.startsWith("http://www.routeconverter.com/catalog/");
    }

    public void upload(String username, String password, String fileUrl, String name, String description) throws IOException {
        routeCatalog.setAuthentication(username, password);

        String categoryUrl = "http://www.routeconverter.com/catalog/categories/_Upload/"; // TODO let the user decide

        // if this is a local file: upload it to the RouteCatalog
        File file = Files.toFile(fileUrl);
        if (file != null) {
            fileUrl = routeCatalog.addFile(file);
            routeCatalog.addRoute(categoryUrl, description, fileUrl);

            // already part of RouteCatalog?
        } else if (isOriginOf(fileUrl)) {
            String routeUrl = "http://www.routeconverter.com/catalog/routes/9999999999.gpx/"; // TODO extract from file url
            routeCatalog.updateRoute(categoryUrl, routeUrl, description, fileUrl);

            // URL to external route
        } else {
            routeCatalog.addRoute(categoryUrl, description, fileUrl);
        }
    }
}
