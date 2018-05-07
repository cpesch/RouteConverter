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
import slash.navigation.routes.remote.binding.RouteType;

import java.io.IOException;
import java.util.Objects;

/**
 * Represents a route on the server which is transferred via {@link RemoteCatalog}
 * and represented with GPX documents.
 *
 * @author Christian Pesch
 */

public class RemoteRoute implements Route {
    private final RemoteCategory category;
    private final String href;
    private String creator, description, url;
    private RouteType routeType;
    private boolean fromCategory;

    public RemoteRoute(RemoteCategory category, String href) {
        this.category = category;
        this.href = href;
    }

    public RemoteRoute(RemoteCategory category, String href, String description, String creator, String url) {
        this(category, href);
        this.creator = creator;
        this.description = description;
        this.url = url;
        fromCategory = true;
    }

    private RemoteCatalog getCatalog() {
        return category.getCatalog();
    }

    public String getHref() {
        return href;
    }

    private synchronized RouteType getRouteType() throws IOException {
        if (routeType == null) {
            CatalogType catalogType = getCatalog().fetch(getHref());
            routeType = catalogType.getRoute();

            // avoid subsequent NullPointerExceptions on server errors
            if (routeType == null)
                routeType = new RouteType();
        }
        return routeType;
    }

    private synchronized void invalidate() {
        category.invalidate();
        creator = null;
        description = null;
        fromCategory = false;
    }

    /*for tests*/Category getCategory() {
        return category;
    }

    public synchronized String getName() throws IOException {
        return getDescription();
    }

    public synchronized String getDescription() throws IOException {
        if (fromCategory)
            return description;
        return getRouteType().getDescription();
    }

    public synchronized String getCreator() throws IOException {
        if (fromCategory)
            return creator;
        return getRouteType().getCreator();
    }

    public synchronized String getUrl() throws IOException {
        if (fromCategory)
            return url;
        return getRouteType().getUrl();
    }

    public void update(Category parent, String description) throws IOException {
        getCatalog().updateRoute(getHref(), parent.getHref(), description, null, null);
        invalidate();
        ((RemoteCategory)parent).invalidate();
    }

    public void delete() throws IOException {
        getCatalog().deleteRoute(getHref());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteRoute that = (RemoteRoute) o;
        return Objects.equals(getCategory(), that.getCategory()) &&
                Objects.equals(getHref(), that.getHref());
    }

    public int hashCode() {
        return Objects.hash(getCategory(), getHref());
    }

    public String toString() {
        return  getClass().getSimpleName() + "[category=" + category + ", href=" + href +
                ", creator=" + creator + ", description=" + description + ", url=" + url +
                ", fromCategory=" + fromCategory + "]";
    }
}
