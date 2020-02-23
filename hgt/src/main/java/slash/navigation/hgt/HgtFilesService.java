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

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Encapsulates access to all services providing HGT files.
 *
 * @author Christian Pesch
 */

public class HgtFilesService {
    private final List<HgtFiles> hgtFiles = new ArrayList<>();
    private static final Set<String> DATASOURCE_URIS = new HashSet<>(asList(
            "srtm3",
            "srtm1",
            "sonny3",
            "sonny1",
            "ferranti3",
            "ferranti1"
    ));

    private DataSourceManager dataSourceManager;

    public HgtFilesService(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public synchronized void initialize() {
        hgtFiles.clear();
        for (DataSource dataSource : dataSourceManager.getDataSourceService().getDataSources()) {
            if (DATASOURCE_URIS.contains(dataSource.getId()))
                hgtFiles.add(new HgtFiles(dataSource, dataSourceManager.getDownloadManager()));
        }
    }

    public synchronized List<HgtFiles> getHgtFiles() {
        return hgtFiles;
    }

    public synchronized void dispose() {
        for (HgtFiles hgtFile : getHgtFiles())
            hgtFile.dispose();
    }
}
