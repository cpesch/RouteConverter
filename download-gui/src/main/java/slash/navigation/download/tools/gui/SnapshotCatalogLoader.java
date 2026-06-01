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
package slash.navigation.download.tools.gui;

import jakarta.xml.bind.JAXBException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.download.Checksum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static slash.navigation.datasources.DataSourceManager.loadAllDataSources;

/**
 * Loads snapshot metadata from a local SnapshotCatalog directory.
 *
 * @author Christian Pesch
 */
public class SnapshotCatalogLoader {
    public SnapshotCatalogData load(Path snapshotRoot) throws IOException, JAXBException {
        Path dataSourcesDirectory = resolveDataSourcesDirectory(snapshotRoot);
        if (!Files.isDirectory(dataSourcesDirectory))
            throw new IOException("Data sources directory does not exist: " + dataSourcesDirectory);

        DataSourceService dataSourceService = loadAllDataSources(dataSourcesDirectory.toFile());
        int editionCount = loadEditionCount(snapshotRoot);

        Map<String, SnapshotJobInfo> jobInfoById = new LinkedHashMap<>();
        for (DataSource dataSource : dataSourceService.getDataSources()) {
            jobInfoById.put(dataSource.getId(), asSnapshotJobInfo(dataSource, dataSourcesDirectory));
        }
        return new SnapshotCatalogData(snapshotRoot, dataSourcesDirectory, editionCount,
                dataSourceService.getDataSources().size(), jobInfoById);
    }

    private Path resolveDataSourcesDirectory(Path snapshotRoot) {
        Path directChild = snapshotRoot.resolve("datasources");
        return Files.isDirectory(directChild) ? directChild : snapshotRoot;
    }

    private int loadEditionCount(Path snapshotRoot) throws IOException, JAXBException {
        Path editionsDirectory = snapshotRoot.resolve("editions");
        if (!Files.isDirectory(editionsDirectory))
            return 0;

        DataSourceService editionService = loadAllDataSources(editionsDirectory.toFile());
        return editionService.getEditions().size();
    }

    private SnapshotJobInfo asSnapshotJobInfo(DataSource dataSource, Path dataSourcesDirectory) {
        int fileCount = dataSource.getFiles().size();
        int mapCount = dataSource.getMaps().size();
        int themeCount = dataSource.getThemes().size();
        int downloadableCount = fileCount + mapCount + themeCount;
        long totalSize = sumDownloadableSizes(dataSource.getFiles()) +
                sumDownloadableSizes(dataSource.getMaps()) +
                sumDownloadableSizes(dataSource.getThemes());
        Path snapshotFile = dataSourcesDirectory.resolve(dataSource.getId() + ".xml");
        return new SnapshotJobInfo(dataSource, dataSource.getSource(),
                fileCount, mapCount, themeCount, downloadableCount, totalSize, snapshotFile);
    }

    private long sumDownloadableSizes(List<? extends Downloadable> downloadables) {
        long result = 0;
        for (Downloadable downloadable : downloadables) {
            Checksum checksum = downloadable.getLatestChecksum();
            if (checksum != null && checksum.getContentLength() != null)
                result += checksum.getContentLength();
        }
        return result;
    }
}

