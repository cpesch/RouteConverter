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

package slash.navigation.catalog.domain;

import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.LinkType;
import slash.navigation.gpx.binding11.RteType;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category on the server which is transferred via {@link RouteService}
 * and represented with GPX documents.
 *
 * @author Christian Pesch
 */

public class Category {
    private final RouteService routeService;
    private String url;
    private GpxType gpx;

    public Category(RouteService routeService, String url) {
        this.routeService = routeService;
        this.url = url;
    }

    private synchronized GpxType getGpx() throws IOException {
        if (gpx == null) {
            gpx = routeService.fetchGpx(url);
        }
        return gpx;
    }

    private synchronized void invalidate() {
        gpx = null;
    }

    private synchronized void recursiveInvalidate() {
        for(Category category : getCachedSubCategories())
            category.recursiveInvalidate();
        invalidate();
    }

    private List<Category> getCachedSubCategories() {
        List<Category> categories = new ArrayList<Category>();
        if (gpx != null)
            for (LinkType linkType : gpx.getMetadata().getLink()) {
                categories.add(new Category(routeService, linkType.getHref()));
            }
        return categories;
    }


    public String getName() throws IOException {
        return getGpx().getMetadata().getName();
    }

    public String getDescription() throws IOException {
        return getGpx().getMetadata().getDesc();
    }

    public List<Category> getSubCategories() throws IOException {
        List<Category> categories = new ArrayList<Category>();
        for (LinkType linkType : getGpx().getMetadata().getLink()) {
            categories.add(new Category(routeService, linkType.getHref()));
        }
        return categories;
    }

    public Category getSubCategory(String name) throws IOException {
        for (Category category : getSubCategories()) {
            if (category.getName().equals(name))
                return category;
        }
        return null;
    }

    public List<Route> getRoutes() throws IOException {
        List<Route> routes = new ArrayList<Route>();
        for (RteType rteType : getGpx().getRte()) {
            routes.add(new Route(routeService, rteType.getLink().get(0).getHref(), rteType.getName(), rteType.getSrc(), rteType.getDesc()));
        }
        return routes;
    }

    public void updateCategory(Category parent, String name) throws IOException {
        url = routeService.updateCategory(url, parent != null ? parent.url : null, name);
        recursiveInvalidate();
    }

    public void delete() throws IOException {
        routeService.deleteCategory(url);
    }

    public Category addSubCategory(String name) throws IOException {
        String resultUrl = routeService.addCategory(url, name);
        invalidate();
        return new Category(routeService, resultUrl);
    }

    public Route addRoute(String description, File file) throws IOException {
        String resultUrl = routeService.addRouteAndFile(url, description, file);
        invalidate();
        return new Route(routeService, resultUrl);
    }

    public Route addRoute(String description, String fileUrl) throws IOException {
        String resultUrl = routeService.addRoute(url, description, fileUrl);
        invalidate();
        return new Route(routeService, resultUrl);
    }

    public void updateRoute(Route route, Category category, String description) throws IOException {
        route.update(category.url, description);
        invalidate();
        category.invalidate();
    }

    public void deleteRoute(Route route) throws IOException {
        route.delete();
        invalidate();
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;

        return routeService.equals(category.routeService) && url.equals(category.url);
    }

    public int hashCode() {
        int result;
        result = routeService.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    public String toString() {
        return super.toString() + "[url=" + url + "]";
    }
}
