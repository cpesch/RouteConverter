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

package slash.navigation.catalog.remote;

import slash.navigation.catalog.domain.Category;
import slash.navigation.catalog.domain.Route;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.LinkType;
import slash.navigation.gpx.binding11.MetadataType;
import slash.navigation.gpx.binding11.RteType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category on the server which is transferred via {@link RemoteCatalog}
 * and represented with GPX documents.
 *
 * @author Christian Pesch
 */

public class RemoteCategory implements Category {
    private final RemoteCatalog catalog;
    private String url;
    private String name;
    private GpxType gpx;

    public RemoteCategory(RemoteCatalog catalog, String url, String name) {
        this.catalog = catalog;
        this.url = url;
        this.name = name;
    }

    RemoteCatalog getCatalog() {
        return catalog;
    }

    public String getUrl() {
        return url;
    }

    private synchronized GpxType getGpx() throws IOException {
        if (gpx == null) {
            gpx = getCatalog().fetchGpx(getUrl());

            // avoid subsequent NullPointerExceptions on server errors
            if (gpx == null) {
                gpx = new GpxType();
                gpx.setMetadata(new MetadataType());
            }
        }
        return gpx;
    }

    synchronized void invalidate() {
        gpx = null;
        name = null;
    }

    private synchronized void recursiveInvalidate() {
        for(RemoteCategory category : getCachedSubCategories())
            category.recursiveInvalidate();
        invalidate();
    }

    private List<RemoteCategory> getCachedSubCategories() {
        List<RemoteCategory> categories = new ArrayList<RemoteCategory>();
        if (gpx != null)
            for (LinkType linkType : gpx.getMetadata().getLink()) {
                categories.add(new RemoteCategory(getCatalog(), linkType.getHref(), linkType.getText()));
            }
        return categories;
    }


    public synchronized String getName() throws IOException {
        if(name != null)
            return name;
        return getGpx().getMetadata().getName();
    }

    public String getDescription() throws IOException {
        return getGpx().getMetadata().getDesc();
    }

    public List<Category> getCategories() throws IOException {
        List<Category> categories = new ArrayList<Category>();
        for (LinkType linkType : getGpx().getMetadata().getLink()) {
            categories.add(new RemoteCategory(getCatalog(), linkType.getHref(), linkType.getText()));
        }
        return categories;
    }

    public List<Route> getRoutes() throws IOException {
        List<Route> routes = new ArrayList<Route>();
        for (RteType rteType : getGpx().getRte()) {
            routes.add(new RemoteRoute(this, rteType.getLink().get(0).getHref(), rteType.getName(), rteType.getSrc(), rteType.getDesc()));
        }
        return routes;
    }

    public Category create(String name) throws IOException {
        String resultUrl = getCatalog().addCategory(getUrl(), name);
        invalidate();
        return new RemoteCategory(getCatalog(), resultUrl, name);
    }

    public void update(Category parent, String name) throws IOException {
        url = getCatalog().updateCategory(getUrl(), parent != null ? parent.getUrl() : null, name);
        this.name = name;
        recursiveInvalidate();
    }

    public void delete() throws IOException {
        getCatalog().deleteCategory(getUrl());
    }

    public Route createRoute(String description, File file) throws IOException {
        String resultUrl = getCatalog().addRouteAndFile(getUrl(), description, file);
        invalidate();
        return new RemoteRoute(this, resultUrl);
    }

    public Route createRoute(String description, String fileUrl) throws IOException {
        String resultUrl = getCatalog().addRoute(getUrl(), description, fileUrl);
        invalidate();
        return new RemoteRoute(this, resultUrl);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteCategory category = (RemoteCategory) o;

        return getCatalog().equals(category.getCatalog()) && getUrl().equals(category.getUrl());
    }

    public int hashCode() {
        int result;
        result = getCatalog().hashCode();
        result = 31 * result + getUrl().hashCode();
        return result;
    }

    public String toString() {
        return  getClass().getSimpleName() + "[url=" + getUrl() + "]";
    }
}
