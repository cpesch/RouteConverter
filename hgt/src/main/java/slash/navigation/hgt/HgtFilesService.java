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
package slash.navigation.hgt;

import slash.navigation.download.DownloadManager;
import slash.navigation.download.datasources.DataSourceService;
import slash.navigation.download.datasources.binding.DatasourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Encapsulates access to all services providing HGT files.
 *
 * @author Christian Pesch
 */

public class HgtFilesService {
    private static final Logger log = Logger.getLogger(HgtFiles.class.getName());
    private final List<HgtFiles> hgtFiles = new ArrayList<HgtFiles>();
    private static final String[] DATASOURCE_URLS = new String[]{
            "srtm3-datasources.xml",
            "srtm1-datasources.xml",
            "ferranti3-datasources.xml",
            "ferranti1-datasources.xml"
    };

    public HgtFilesService(DownloadManager downloadManager) {
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
            hgtFiles.add(new HgtFiles(name, datasourceType.getBaseUrl(), datasourceType.getDirectory(),
                    service.getArchives(name), service.getFiles(name), downloadManager));
        }
    }

    public List<HgtFiles> getHgtFiles() {
        return hgtFiles;
    }

    public void dispose() {
        for (HgtFiles hgtFile : getHgtFiles())
            hgtFile.dispose();
    }
}
