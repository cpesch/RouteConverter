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
package slash.navigation.maps;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.File;
import slash.navigation.datasources.Theme;
import slash.navigation.maps.models.RemoteMapImpl;
import slash.navigation.maps.models.RemoteResourceImpl;
import slash.navigation.maps.models.RemoteThemeImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Encapsulates access to mapsforge .map and theme files.
 *
 * @author Christian Pesch
 */

public class MapFiles {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapFiles.class);
    private static final String BASE_URL_PREFERENCE = "baseUrl";

    private final DataSource dataSource;

    public MapFiles(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getName() {
        return dataSource.getName();
    }

    String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), dataSource.getBaseUrl());
    }

    private String getSubDirectory() {
        return dataSource.getDirectory();
    }

    public List<RemoteResource> getResources() {
        List<RemoteResource> result = new ArrayList<>();
        for(File file : dataSource.getFiles()) {
            result.add(new RemoteResourceImpl(getName(), getBaseUrl(), getSubDirectory(), file));
        }
        for(slash.navigation.datasources.Map map : dataSource.getMaps()) {
            result.add(new RemoteMapImpl(getName(), getBaseUrl(), getSubDirectory(), map));
        }
        for(Theme theme : dataSource.getThemes()) {
            result.add(new RemoteThemeImpl(getName(), getBaseUrl(), getSubDirectory(), theme));
        }
        return result;
    }
}
