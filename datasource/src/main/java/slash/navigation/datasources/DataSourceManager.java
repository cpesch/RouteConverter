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

package slash.navigation.datasources;

import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static slash.common.type.CompactCalendar.now;
import static slash.navigation.download.Action.Copy;

/**
 * Encapsulates access to the download of the DataSource XMLs.
 *
 * @author Christian Pesch
 */

public class DataSourceManager {
    private static final Logger log = Logger.getLogger(DataSourceManager.class.getName());
    private static final String EDITIONS = "editions";
    private static final String EDITIONS_URI = "v1/" + EDITIONS + "/";
    private static final String FORMAT_XML = "?format=xml";
    private static final String DOT_XML = ".xml";
    public static final String DOT_ZIP = ".zip";

    private final DownloadManager downloadManager;
    private DataSourceService dataSourceService = new DataSourceService();

    public DataSourceManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public void dispose() {
        dataSourceService.clear();
        getDownloadManager().dispose();
    }

    public void initialize(String edition, java.io.File directory) throws IOException, JAXBException {
        java.io.File file = new File(directory, edition + DOT_XML);
        log.info(format("Initializing edition '%s' from %s", edition, file));
        Edition anEdition = loadEdition(file);
        if(anEdition == null)
            return;

        this.dataSourceService = loadDataSources(anEdition.getDataSources(), directory);
    }

    private Edition loadEdition(java.io.File file) throws IOException, JAXBException {
        if (!file.exists()) {
            log.warning(format("Cannot find edition file %s", file));
            return null;
        }

        DataSourceService service = loadDataSource(file);
        List<Edition> editions = service.getEditions();
        return editions.size() > 0 ? editions.get(0) : null;
    }

    private DataSourceService loadDataSources(List<DataSource> dataSources, java.io.File directory) throws IOException, JAXBException {
        DataSourceService result = new DataSourceService();
        for (DataSource dataSource : new ArrayList<>(dataSources)) {
            java.io.File file = new java.io.File(directory, dataSource.getId() + DOT_XML);
            log.info(format("Initializing data source from %s", file));
            if (!file.exists()) {
                log.warning(format("Cannot find data source file %s", file));
                continue;
            }

            try (InputStream inputStream = new FileInputStream(file)) {
                result.load(inputStream);
            }
        }
        return result;
    }

    public void update(String edition, String url, java.io.File directory) throws IOException, JAXBException {
        log.info(format("Updating edition '%s' from %s to %s", edition, url, directory));
        java.io.File file = new java.io.File(directory, edition + DOT_XML);
        downloadEdition(edition, url, file);
        Edition anEdition = loadEdition(file);
        if(anEdition == null)
            return;

        downloadDataSources(anEdition.getDataSources(), directory);
        this.dataSourceService = loadDataSources(anEdition.getDataSources(), directory);
        downloadManager.setLastSync(now());
    }

    public void downloadRoot(String url, java.io.File directory) {
        String editionsUrl = url + EDITIONS_URI + FORMAT_XML;
        java.io.File file = new java.io.File(directory, EDITIONS + DOT_XML);
        Download download = downloadManager.queueForDownload("RouteConverter Editions", editionsUrl, Copy, null,
                new FileAndChecksum(file, null), null);
        downloadManager.waitForCompletion(singletonList(download));
    }

    private void downloadEdition(String edition, String url, java.io.File file) {
        String editionUrl = url + EDITIONS_URI + edition + "/" + FORMAT_XML;
        Download download = downloadManager.queueForDownload("RouteConverter Edition: " + edition, editionUrl, Copy,
                null, new FileAndChecksum(file, null), null);
        downloadManager.waitForCompletion(singletonList(download));
    }

    public void downloadEditions(List<Edition> editions, java.io.File directory) throws JAXBException, FileNotFoundException {
        List<Download> downloads = new ArrayList<>();
        for (Edition edition : editions) {
            String editionUrl = edition.getHref() + FORMAT_XML;
            java.io.File file = new java.io.File(directory, edition.getId() + DOT_XML);
            Download download = downloadManager.queueForDownload("RouteConverter Edition: " + edition.getId(),
                    editionUrl, Copy, null, new FileAndChecksum(file, null), null);
            downloads.add(download);
        }
        downloadManager.waitForCompletion(downloads);
    }

    public void downloadDataSources(List<DataSource> dataSources, java.io.File directory) throws JAXBException, FileNotFoundException {
        List<Download> downloads = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            String datasourceUrl = dataSource.getHref() + FORMAT_XML;
            java.io.File file = new java.io.File(directory, dataSource.getId() + DOT_XML);
            Download download = downloadManager.queueForDownload("RouteConverter DataSource: " + dataSource.getId(),
                    datasourceUrl, Copy, null, new FileAndChecksum(file, null), null);
            downloads.add(download);
        }
        downloadManager.waitForCompletion(downloads);
    }

    private static DataSourceService loadDataSource(java.io.File file) throws IOException, JAXBException {
        DataSourceService result = new DataSourceService();
        try (InputStream inputStream = new FileInputStream(file)) {
            result.load(inputStream);
        }
        return result;
    }

    public static DataSourceService loadAllDataSources(java.io.File directory) throws IOException, JAXBException {
        DataSourceService result = new DataSourceService();
        java.io.File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(java.io.File dir, String name) {
                return name.endsWith(DOT_XML);
            }
        });
        if (files != null) {
            for (java.io.File file : files) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    result.load(inputStream);
                }
            }
        }
        return result;
    }
}
