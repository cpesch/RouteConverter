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

import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.type.CompactCalendar.now;
import static slash.navigation.download.Action.Copy;

/**
 * Encapsulates access to the download of the DataSource XMLs.
 *
 * @author Christian Pesch
 */

public class DataSourceManager {
    private static final Logger log = Logger.getLogger(DataSourceManager.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(DataSourceManager.class);
    private static final String DIRECTORY_PREFERENCE = "directory";

    private final DownloadManager downloadManager;
    private final DataSourceService dataSourceService = new DataSourceService();

    public DataSourceManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void initialize(String edition) throws IOException, JAXBException {
        initializeEdition(edition);
        initializeDataSources();
        downloadManager.setLastSync(now());
    }

    private void initializeEdition(String edition) throws JAXBException, FileNotFoundException {
        String uri = edition.toLowerCase() + ".xml";
        String url = System.getProperty("datasources", "http://www.routeconverter.com/datasources/") + "edition/" + uri;
        log.info(format("Downloading edition '%s'", url));

        Download download = downloadManager.queueForDownload("RouteConverter " + edition + " Edition: Catalog of Data Sources", url, Copy,
                null, new FileAndChecksum(new java.io.File(getTarget(), uri), null), null);
        downloadManager.waitForCompletion(asList(download));

        java.io.File target = download.getFile().getFile();
        if (!target.exists()) {
            log.warning(format("Cannot find %s to load '%s' data", target, download.getDescription()));
            return;
        }
        dataSourceService.load(new FileInputStream(download.getFile().getFile()));
    }

    private void initializeDataSources() throws JAXBException, FileNotFoundException {
        List<Download> downloads = new ArrayList<>();
        for(DataSource dataSource : dataSourceService.getDataSources()) {
            for(File file : dataSource.getFiles()) {
                String url = dataSource.getBaseUrl() + file.getUri();
                log.info(format("Downloading data source '%s'", url));

                Download download = downloadManager.queueForDownload(dataSource.getName() + ": Data Source " + file.getUri(),
                        url, Copy, null, new FileAndChecksum(new java.io.File(getTarget(), file.getUri().toLowerCase()), file.getLatestChecksum()),
                        null);
                downloads.add(download);
            }
        }
        downloadManager.waitForCompletion(downloads);

        for(Download download : downloads) {
            java.io.File target = download.getFile().getFile();
            if (!target.exists()) {
                log.warning(format("Cannot find %s to load '%s' data", target, download.getDescription()));
                continue;
            }
            dataSourceService.load(new FileInputStream(target));
        }
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    private java.io.File getTarget() {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if(!f.exists())
            directoryName = getApplicationDirectory("datasources").getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    public DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }
}
