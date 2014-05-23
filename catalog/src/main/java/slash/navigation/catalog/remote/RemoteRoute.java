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
import slash.navigation.gpx.binding11.RteType;

import java.io.IOException;
import java.net.URL;

/**
 * Represents a route on the server which is transferred via {@link RemoteCatalog}
 * and represented with GPX documents.
 *
 * @author Christian Pesch
 */

public class RemoteRoute implements Route {
    private final RemoteCategory category;
    private final String url;
    private String name, creator, description;
    private boolean fromCategory = false;
    private GpxType gpx;

    public RemoteRoute(RemoteCategory category, String url) {
        this.category = category;
        this.url = url;
    }

    public RemoteRoute(RemoteCategory category, String url, String name, String creator, String description) {
        this.category = category;
        this.url = url;
        this.name = name;
        this.creator = creator;
        this.description = description;
        fromCategory = true;
    }

    private RemoteCatalog getCatalog() {
        return category.getCatalog();
    }

    public String getUrl() {
        return url;
    }

    private synchronized GpxType getGpx() throws IOException {
        if (gpx == null)
            gpx = getCatalog().fetchGpx(getUrl());
        return gpx;
    }

    private synchronized void invalidate() {
        category.invalidate();
        gpx = null;
        name = null;
        creator = null;
        description = null;
        fromCategory = false;
    }

    private RteType getRte() throws IOException {
        GpxType gpx = getGpx();
        return gpx != null ? gpx.getRte().get(0) : null;
    }

    public synchronized String getName() throws IOException {
        if (fromCategory)
            return name;
        return getRte().getName();
    }

    public synchronized String getDescription() throws IOException {
        if (fromCategory)
            return description;
        return getRte().getDesc();
    }

    public synchronized String getCreator() throws IOException {
        if (fromCategory)
            return creator;
        return getRte().getSrc();
    }

    private String getRteLinkHref() throws IOException {
        RteType rte = getRte();
        return rte != null ? rte.getLink().get(0).getHref() : null;
    }

    public URL getDataUrl() throws IOException {
        String rteLinkHref = getRteLinkHref();
        return rteLinkHref != null ? new URL(rteLinkHref) : null;
    }

    public void update(Category parent, String description) throws IOException {
        getCatalog().updateRoute(parent.getUrl(), getUrl(), description, getRteLinkHref());
        invalidate();
        ((RemoteCategory)parent).invalidate();
    }

    public void delete() throws IOException {
        getCatalog().deleteRoute(getUrl());
        getCatalog().deleteFile(getRteLinkHref());
        invalidate();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteRoute route = (RemoteRoute) o;

        return category.equals(route.category) && getUrl().equals(route.getUrl());
    }

    public int hashCode() {
        int result;
        result = category.hashCode();
        result = 31 * result + getUrl().hashCode();
        return result;
    }

    public String toString() {
        return  getClass().getSimpleName() + "[category=" + category + ", url=" + url +
                ", name=" + name + ", creator=" + creator + ", description=" + description +
                ", fromCategory=" + fromCategory + "]";
    }
}
