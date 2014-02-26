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

import slash.navigation.download.DownloadManager;
import slash.navigation.download.datasources.File;
import slash.navigation.download.datasources.Fragment;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import static java.lang.String.format;

/**
 * Encapsulates access to mapsforge .map and theme files.
 *
 * @author Christian Pesch
 */

public class MapFiles {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapFiles.class);
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";

    private final Map<java.io.File, RandomAccessFile> randomAccessFileCache = new HashMap<java.io.File, RandomAccessFile>();
    private final String name, baseUrl, directory;
    private final Map<String, Fragment> archiveMap;
    private final Map<String, File> fileMap;
    private final DownloadManager downloadManager;

    public MapFiles(String name, String baseUrl, String directory,
                    Map<String, Fragment> archiveMap, Map<String, File> fileMap,
                    DownloadManager downloadManager) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.directory = directory;
        this.archiveMap = archiveMap;
        this.fileMap = fileMap;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return name;
    }

    String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), baseUrl);
    }

    private java.io.File getDirectory() {
        String directoryName = preferences.get(DIRECTORY_PREFERENCE + getName(),
                new java.io.File(System.getProperty("user.home"), ".routeconverter/" + directory).getAbsolutePath());
        java.io.File directory = new java.io.File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs())
                throw new IllegalArgumentException(format("Cannot create '%s' directory '%s'", getName(), directory));
        }
        return directory;
    }

    public List<Resource> getResources() {
        List<Resource> result = new ArrayList<Resource>();
        for(final File file : fileMap.values()) {
            result.add(new Resource() {
                public String getDescription() {
                    return file.getUri();
                }
                public String getUrl() {
                    return file.getUri();
                }
            });
        }
        for(final Fragment fragment : archiveMap.values()) {
            result.add(new Resource() {
                public String getDescription() {
                    return fragment.getUri();
                }
                public String getUrl() {
                    return fragment.getUri();
                }
            });
        }
        return result;
    }
}
