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
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.download.Action.Flatten;

/**
 * Encapsulates access to the download of the DataSource XMLs.
 *
 * @author Christian Pesch
 */

public class DataSourceManager {
    private static final Logger log = Logger.getLogger(DataSourceManager.class.getName());
    private static final String EDITIONS = "editions";
    public static final String V1 = "v1/";
    private static final String EDITIONS_URI = V1 + EDITIONS + "/";
    public static final String DATASOURCES_URI = V1 + "datasources/";
    public static final String FORMAT_XML = "?format=xml";
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

        updateQueueFromDataSources();
    }

    public void downloadRoot(String url, java.io.File directory) {
        String editionsUrl = url + EDITIONS_URI + FORMAT_XML;
        java.io.File file = new java.io.File(directory, EDITIONS + DOT_XML);
        Download download = downloadManager.queueForDownload("RouteConverter Editions", editionsUrl, Copy,
                new FileAndChecksum(file, null), null);
        downloadManager.waitForCompletion(singletonList(download));
    }

    private void downloadEdition(String edition, String url, java.io.File file) {
        String editionUrl = url + EDITIONS_URI + edition + "/" + FORMAT_XML;
        Download download = downloadManager.queueForDownload("RouteConverter Edition: " + edition, editionUrl, Copy,
                new FileAndChecksum(file, null), null);
        downloadManager.waitForCompletion(singletonList(download));
    }

    // for {@link SnapshotCatalog}
    public void downloadEditions(List<Edition> editions, java.io.File directory) {
        List<Download> downloads = new ArrayList<>();
        for (Edition edition : editions) {
            String editionUrl = edition.getHref() + FORMAT_XML;
            java.io.File file = new java.io.File(directory, edition.getId() + DOT_XML);
            Download download = downloadManager.queueForDownload("RouteConverter Edition: " + edition.getId(),
                    editionUrl, Copy, new FileAndChecksum(file, null), null);
            downloads.add(download);
        }
        downloadManager.waitForCompletion(downloads);
    }

    public void downloadDataSources(List<DataSource> dataSources, java.io.File directory) {
        List<Download> downloads = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            String datasourceUrl = dataSource.getHref() + FORMAT_XML;
            java.io.File file = new java.io.File(directory, dataSource.getId() + DOT_XML);
            Download download = downloadManager.queueForDownload("Datasource: " + dataSource.getId(),
                    datasourceUrl, Copy, new FileAndChecksum(file, null), null);
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

    // for {@link SnapshotCatalog}
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

    private void scanForFilesMissingInQueue(File directory) throws IOException {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    Downloadable downloadable = dataSourceService.getDownloadable(file);
                    if(downloadable != null) {
                        DataSource dataSource = downloadable.getDataSource();
                        addOrUpdateInQueue(dataSource, downloadable);
                    } else
                        log.fine("Cannot find downloadable for " + file);
                } else if (file.isDirectory())
                    scanForFilesMissingInQueue(file);
            }
        }
    }

    public void scanForFilesMissingInQueue() throws IOException {
        scanForFilesMissingInQueue(getApplicationDirectory());
    }

    private void addOrUpdateInQueue(DataSource dataSource, Downloadable downloadable)  {
        Action action = Action.valueOf(dataSource.getAction());
        File directory = getApplicationDirectory(dataSource.getDirectory());

        File target = directory; // Flatten
        if (action.equals(Copy))
            target = new File(directory, downloadable.getUri());
        else if (action.equals(Extract))
            target = target.getParentFile();

        downloadManager.addOrUpdateInQueue(dataSource.getName() + ": " + downloadable.getUri(),
                dataSource.getBaseUrl() + downloadable.getUri(), action,
                new FileAndChecksum(target, downloadable.getLatestChecksum()),
                asFragments(directory, downloadable.getFragments(), action.equals(Extract)));
    }

    public Download queueForDownload(DataSource dataSource, Downloadable downloadable) {
        Action action = Action.valueOf(dataSource.getAction());
        File directory = getApplicationDirectory(dataSource.getDirectory());
        File target = new File(directory, downloadable.getUri());
        if (action.equals(Extract) || action.equals(Flatten))
            target = ensureDirectory(target.getParentFile());

        return downloadManager.queueForDownload(dataSource.getName() + ": " + downloadable.getUri(),
                dataSource.getBaseUrl() + downloadable.getUri(), action,
                new FileAndChecksum(target, downloadable.getLatestChecksum()),
                asFragments(target, downloadable.getFragments(), false));
    }

    private List<FileAndChecksum> asFragments(File directory, List<Fragment<Downloadable>> fragments,
                                              boolean useDirectoryParent) {
        if(fragments == null)
            return null;

        List<FileAndChecksum> result = new ArrayList<>();
        for (Fragment<Downloadable> fragment : fragments) {
            String key = fragment.getKey();
            File target = new File(useDirectoryParent ? directory.getParentFile() : directory, key);
            result.add(new FileAndChecksum(target, fragment.getLatestChecksum()));
        }
        return result;
    }

    private void updateQueueFromDataSources() {
        for(Download download : downloadManager.getModel().getDownloads()) {
            Downloadable downloadable = dataSourceService.getDownloadable(download.getUrl());
            if(downloadable != null) {
                DataSource dataSource = downloadable.getDataSource();
                addOrUpdateInQueue(dataSource, downloadable);
            } else
                log.fine("Cannot find downloadable for " + download.getUrl());
        }
    }
}
