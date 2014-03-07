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

import slash.navigation.download.datasources.DataSourceService;
import slash.navigation.download.datasources.binding.DatasourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Encapsulates access to all services providing map and theme files.
 *
 * @author Christian Pesch
 */

public class MapFilesService {
    private static final Logger log = Logger.getLogger(MapFilesService.class.getName());
    private final List<MapFiles> mapFiles = new ArrayList<MapFiles>();
    private static final String[] DATASOURCE_URLS = new String[]{
            "freizeitkarte-maps-datasources.xml",
            "freizeitkarte-themes-datasources.xml",
            "mapsforge-maps-datasources.xml",
            "openandromaps-maps-datasources.xml",
            "openandromaps-themes-datasources.xml"
    };

    public MapFilesService() {
        DataSourceService service = new DataSourceService();
        for (String datasourceUrl : DATASOURCE_URLS) {
            try {
                service.load(getClass().getResourceAsStream(datasourceUrl));
            } catch (Exception e) {
                log.severe(format("Cannot load '%s': %s", datasourceUrl, e.getMessage()));
            }
        }

        for (DatasourceType datasourceType : service.getDatasourceTypes()) {
            String name = datasourceType.getName();
            mapFiles.add(new MapFiles(name, datasourceType.getBaseUrl(), datasourceType.getDirectory(),
                    service.getArchives(name), service.getFiles(name)));
        }
    }

    public List<MapFiles> getMapFiles() {
        return mapFiles;
    }

    public List<RemoteResource> getResources() {
        List<RemoteResource> result = new ArrayList<RemoteResource>();
        for (MapFiles files : getMapFiles()) {
            result.addAll(files.getResources());
        }
        return result;
    }
}
