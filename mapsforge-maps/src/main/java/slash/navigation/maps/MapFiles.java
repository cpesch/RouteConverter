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

import slash.navigation.download.datasources.File;
import slash.navigation.download.datasources.Fragment;
import slash.navigation.maps.models.RemoteResourceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Encapsulates access to mapsforge .map and theme files.
 *
 * @author Christian Pesch
 */

public class MapFiles {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapFiles.class);
    private static final String BASE_URL_PREFERENCE = "baseUrl";

    private final String name, baseUrl, subDirectory;
    private final Map<String, Fragment> archiveMap;
    private final Map<String, File> fileMap;

    public MapFiles(String name, String baseUrl, String subDirectory,
                    Map<String, Fragment> archiveMap, Map<String, File> fileMap) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.subDirectory = subDirectory;
        this.archiveMap = archiveMap;
        this.fileMap = fileMap;
    }

    private String getName() {
        return name;
    }

    private String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), baseUrl);
    }

    private String getSubDirectory() {
        return subDirectory;
    }

    public List<RemoteResource> getResources() {
        List<RemoteResource> result = new ArrayList<RemoteResource>();
        for(final File file : fileMap.values()) {
            result.add(new RemoteResourceImpl(getName(), getBaseUrl(), getSubDirectory(), file));
        }
        for(final Fragment fragment : archiveMap.values()) {
            result.add(new RemoteResourceImpl(getName(), getBaseUrl(), getSubDirectory(), fragment));
        }
        return result;
    }
}
