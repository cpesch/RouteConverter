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
package slash.navigation.maps.mapsforge.impl;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.maps.mapsforge.RemoteResource;

/**
 * The implementation of a {@link RemoteResource}.
 *
 * @author Christian Pesch
 */

public class RemoteResourceImpl implements RemoteResource {
    private final DataSource datasource;
    private final Downloadable downloadable;

    public RemoteResourceImpl(DataSource datasource, Downloadable downloadable) {
        this.datasource = datasource;
        this.downloadable = downloadable;
    }

    public DataSource getDataSource() {
        return datasource;
    }

    public Downloadable getDownloadable() {
        return downloadable;
    }

    public String getDescription() {
        return getDownloadable().getUri();
    }

    public String getUrl() {
        return datasource.getBaseUrl() + downloadable.getUri();
    }
}
