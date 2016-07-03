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

package slash.navigation.routes.remote;

import slash.navigation.routes.Category;
import slash.navigation.routes.Route;
import slash.navigation.routes.remote.binding.CatalogType;
import slash.navigation.routes.remote.binding.CategoryType;
import slash.navigation.routes.remote.binding.RouteType;

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
    private final String href;
    private String name;
    private CategoryType categoryType;

    public RemoteCategory(RemoteCatalog catalog, String href) {
        this.catalog = catalog;
        this.href = href;
    }

    public RemoteCategory(RemoteCatalog catalog, String href, String name) {
        this(catalog, href);
        this.name = name;
    }

    RemoteCatalog getCatalog() {
        return catalog;
    }

    public String getHref() {
        return href;
    }

    private synchronized CategoryType getCategoryType() throws IOException {
        if (categoryType == null) {
            CatalogType catalogType = getCatalog().fetch(getHref());
            categoryType = catalogType.getCategory();

            // avoid subsequent NullPointerExceptions on server errors
            if (categoryType == null)
                categoryType = new CategoryType();
        }
        return categoryType;
    }

    synchronized void invalidate() {
        categoryType = null;
        name = null;
    }

    private synchronized void recursiveInvalidate() {
        for (RemoteCategory category : getCachedSubCategories())
            category.recursiveInvalidate();
        invalidate();
    }

    private List<RemoteCategory> getCachedSubCategories() {
        List<RemoteCategory> categories = new ArrayList<>();
        if (categoryType != null)
            for (CategoryType subCategory : categoryType.getCategory()) {
                categories.add(new RemoteCategory(getCatalog(), subCategory.getHref(), subCategory.getName()));
            }
        return categories;
    }

    /*for tests*/Category getParent() throws IOException {
        return new RemoteCategory(getCatalog(), getCategoryType().getParent());
    }

    public synchronized String getName() throws IOException {
        if (name != null)
            return name;
        return getCategoryType().getName();
    }

    public List<Category> getCategories() throws IOException {
        List<Category> categories = new ArrayList<>();
        for (CategoryType subCategory : getCategoryType().getCategory()) {
            categories.add(new RemoteCategory(getCatalog(), subCategory.getHref(), subCategory.getName()));
        }
        return categories;
    }

    public List<Route> getRoutes() throws IOException {
        List<Route> routes = new ArrayList<>();
        for (RouteType route : getCategoryType().getRoute()) {
            routes.add(new RemoteRoute(this, route.getHref(), route.getDescription(), route.getCreator(), route.getUrl()));
        }
        return routes;
    }

    public Category create(String name) throws IOException {
        String resultUrl = getCatalog().addCategory(getHref(), name);
        invalidate();
        return new RemoteCategory(getCatalog(), resultUrl);
    }

    public synchronized void update(Category parent, String name) throws IOException {
        getCatalog().updateCategory(getHref(), parent != null ? parent.getHref() : getParent().getHref(), name);
        this.name = name;
        recursiveInvalidate();
    }

    public void delete() throws IOException {
        getCatalog().deleteCategory(getHref());
    }

    public Route createRoute(String description, File localFile) throws IOException {
        String fileUrl = getCatalog().addFile(localFile);
        String routeUrl = getCatalog().addRoute(getHref(), description, fileUrl, null);
        invalidate();
        return new RemoteRoute(this, routeUrl);
    }

    public Route createRoute(String description, String remoteUrl) throws IOException {
        String routeUrl = getCatalog().addRoute(getHref(), description, null, remoteUrl);
        invalidate();
        return new RemoteRoute(this, routeUrl);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteCategory category = (RemoteCategory) o;

        return getCatalog().equals(category.getCatalog()) && getHref().equals(category.getHref());
    }

    public int hashCode() {
        int result;
        result = getCatalog().hashCode();
        result = 31 * result + getHref().hashCode();
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[href=" + getHref() + "]";
    }
}
