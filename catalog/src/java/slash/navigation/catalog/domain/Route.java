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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.catalog.domain;

import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.RteType;

import java.io.File;
import java.io.IOException;

/**
 * Represents a route on the server which is transferred via {@link RouteService}
 * and represented with GPX documents.
 *
 * @author Christian Pesch
 */

public class Route {
    private RouteService routeService;
    private String url;
    private String name, creator, description;
    private boolean fromCategory = false;
    private GpxType gpx;
    private File file;


    public Route(RouteService routeService, String url) {
        this.routeService = routeService;
        this.url = url;
    }

    public Route(RouteService routeService, String url, String name, String creator, String description) {
        this.routeService = routeService;
        this.url = url;
        this.name = name;
        this.creator = creator;
        this.description = description;
        fromCategory = true;
    }

    private synchronized GpxType getGpx() throws IOException {
        if (gpx == null) {
            gpx = routeService.fetchGpx(url);
        }
        return gpx;
    }

    private synchronized void invalidate() {
        gpx = null;
        name = null;
        creator = null;
        description = null;
        fromCategory = false;
    }

    private RteType getRte() throws IOException {
        return getGpx().getRte().get(0);
    }

    public String getName() throws IOException {
        if (fromCategory)
            return name;
        return getRte().getName();
    }

    public String getCreator() throws IOException {
        if (fromCategory)
            return creator;
        return getRte().getSrc();
    }

    public String getDescription() throws IOException {
        if (fromCategory)
            return description;
        return getRte().getDesc();
    }

    public String getFileUrl() throws IOException {
        return getRte().getLink().get(0).getHref();
    }

    public synchronized File getFile() throws IOException {
        if(file == null) {
            this.file = routeService.fetchFile(getFileUrl());
        }
        return file;
    }

    public void update(String categoryUrl, String description) throws IOException {
        routeService.updateRoute(categoryUrl, url, description, getFileUrl());
        invalidate();
    }

    public void delete() throws IOException {
        routeService.deleteRoute(url);
        routeService.deleteFile(getFileUrl());
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Route route = (Route) o;

        return routeService.equals(route.routeService) && url.equals(route.url);
    }

    public int hashCode() {
        int result;
        result = routeService.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    public String toString() {
        return super.toString() + "[url=" + url + ", name=" + name + ", creator=" + creator + ", description=" + description + ", fromCategory=" + fromCategory + "]";
    }
}
