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
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.maps.mapsforge.RemoteMap;
import slash.navigation.maps.mapsforge.RemoteTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates access to all services providing map and theme files.
 *
 * @author Christian Pesch
 */

public class MapFilesService {
    private final List<MapFiles> mapFiles = new ArrayList<>();
    private DataSourceManager dataSourceManager;

    public MapFilesService(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public void initialize() {
        DataSourceService dataSourceService = dataSourceManager.getDataSourceService();

        for (DataSource dataSource : dataSourceService.getDataSources()) {
            mapFiles.add(new MapFiles(dataSource));
        }
    }

    private List<MapFiles> getMapFiles() {
        return mapFiles;
    }

    public List<RemoteMap> getMaps() {
        List<RemoteMap> result = new ArrayList<>();
        for (MapFiles files : getMapFiles()) {
            result.addAll(files.getMaps());
        }
        return result;
    }

    public List<RemoteTheme> getThemes() {
        List<RemoteTheme> result = new ArrayList<>();
        for (MapFiles files : getMapFiles()) {
            result.addAll(files.getThemes());
        }
        return result;
    }
}
