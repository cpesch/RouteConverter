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

import slash.navigation.datasources.DataSourceService;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.download.DownloadManager;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Encapsulates access to all services using HGT files.
 *
 * @author Christian Pesch
 */

public class HgtFilesService {
    private static Logger log = Logger.getLogger(HgtFiles.class.getName());
    private final List<HgtFiles> hgtFiles = new ArrayList<HgtFiles>();

    public HgtFilesService(DownloadManager downloadManager) {
        DataSourceService service = new DataSourceService();
        try {
            service.initialize(getClass().getResourceAsStream("hgt-datasources.xml"));
        } catch (JAXBException e) {
            log.severe("Cannot load HGT files data sources: " + e.getMessage());
            return;
        }

        List<DatasourceType> datasourceTypes = service.getDatasourcesType().getDatasource();
        for (DatasourceType datasourceType : datasourceTypes) {
            String name = datasourceType.getName();
            hgtFiles.add(new HgtFiles(name, datasourceType.getBaseUrl(), service.getMapping(name),
                    datasourceType.getDirectory(), downloadManager));
        }
    }

    public List<HgtFiles> getHgtFiles() {
        return hgtFiles;
    }

    public void dispose() {
        for(HgtFiles hgtFile : getHgtFiles())
            hgtFile.dispose();
    }
}
