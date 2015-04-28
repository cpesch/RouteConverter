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
package slash.navigation.maps.impl;

import slash.navigation.datasources.Downloadable;
import slash.navigation.maps.RemoteResource;

/**
 * The implementation of a {@link RemoteResource}.
 *
 * @author Christian Pesch
 */

public class RemoteResourceImpl implements RemoteResource {
    private final String datasource;
    private final String baseUrl;
    private final String subDirectory;
    private final Downloadable downloadable;

    public RemoteResourceImpl(String datasource, String baseUrl, String subDirectory, Downloadable downloadable) {
        this.datasource = datasource;
        this.baseUrl = baseUrl;
        this.subDirectory = subDirectory;
        this.downloadable = downloadable;
    }

    public String getDataSource() {
        return datasource;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public Downloadable getDownloadable() {
        return downloadable;
    }

    public String getUrl() {
        return baseUrl + downloadable.getUri();
    }
}
