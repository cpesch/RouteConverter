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

package slash.navigation.download.datasources;

import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.actions.Checksum;
import slash.navigation.download.datasources.binding.DatasourceType;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static java.util.Arrays.asList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.Action.Copy;

/**
 * Encapsulates access to the download of the DataSource XMLs.
 *
 * @author Christian Pesch
 */

public class DataSourceManager {
    private static final Preferences preferences = Preferences.userNodeForPackage(DataSourceManager.class);
    private static final String DIRECTORY_PREFERENCE = "directory";

    private final DownloadManager downloadManager;

    public DataSourceManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void initialize(String edition) throws IOException, JAXBException {
        java.io.File target = new java.io.File(getTarget(), edition.toLowerCase() + ".xml");
        Download download = downloadManager.queueForDownload(edition + " Edition datasources", "http://localhost:8000/datasources/edition/" + edition.toLowerCase() + ".xml",
                new Checksum(null, null, fromMillis(target.lastModified())), Copy, target);
        downloadManager.waitForCompletion(asList(download));

        DataSourceService service = new DataSourceService();
        service.load(new FileInputStream(download.getTarget()));
        initializeServices(service);
    }

    private void initializeServices(DataSourceService service) {
        List<Download> downloads = new ArrayList<Download>();
        for(DatasourceType dataSourceType : service.getDatasourceTypes()) {
            for(File file : service.getFiles(dataSourceType.getName()).values()) {
                Download download = downloadManager.queueForDownload(dataSourceType.getName() + ": Datasource " + file.getUri(),
                        dataSourceType.getBaseUrl() + file.getUri(), file.getChecksum(), Copy,
                        new java.io.File(getTarget(), file.getUri()));
                downloads.add(download);
            }
        }
        downloadManager.waitForCompletion(downloads);
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
}
