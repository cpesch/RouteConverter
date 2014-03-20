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

package slash.navigation.graphhopper;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.util.PointList;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.actions.Validator;
import slash.navigation.download.datasources.DataSourceService;
import slash.navigation.download.datasources.File;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.graphhopper.util.CmdArgs.read;
import static java.lang.String.format;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.navigation.download.Action.Copy;

/**
 * Encapsulates access to the GraphHopper.
 *
 * @author Christian Pesch
 */

public class GraphHopper implements RoutingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GraphHopper.class);
    private static final Logger log = Logger.getLogger(GraphHopper.class.getName());
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";
    private static final String DATASOURCE_URL = "graphhopper-datasources.xml";

    private Map<String, File> fileMap;
    private String baseUrl, directory;
    private com.graphhopper.GraphHopper hopper;
    private DownloadManager downloadManager;

    public GraphHopper() {
        DataSourceService service = new DataSourceService();
        try {
            service.load(getClass().getResourceAsStream(DATASOURCE_URL));
        } catch (JAXBException e) {
            log.severe(format("Cannot load '%s': %s", DATASOURCE_URL, e.getMessage()));
        }
        this.fileMap = service.getFiles(getName());
        this.baseUrl = service.getDataSource(getName()).getBaseUrl();
        this.directory = service.getDataSource(getName()).getDirectory();
    }

    public void setDownloadManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void initialize() throws IOException {
        String[] args = new String[] {
                "osmreader.osm", new java.io.File(getDirectory(), "europe_germany.ghz").getAbsolutePath()
        };

        hopper = new com.graphhopper.GraphHopper().forDesktop();
        hopper.init(read(args));
        hopper.importOrLoad();
    }

    public String getName() {
        return "GraphHopper";
    }

    private String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE, baseUrl);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to) {
        GHResponse response = hopper.route(new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude()));
        // TODO response.getMillis();
        return new RoutingResult(asPositions(response.getPoints()), (int) response.getDistance());
    }

    private List<NavigationPosition> asPositions(PointList points) {
        List<NavigationPosition> result = new ArrayList<NavigationPosition>();
        for (int i = 0, c = points.getSize(); i < c; i++) {
            result.add(new SimpleNavigationPosition(points.getLongitude(i), points.getLatitude(i)));
        }
        return result;
    }

    private java.io.File getDirectory() {
        String directoryName = preferences.get(DIRECTORY_PREFERENCE, getApplicationDirectory(directory).getAbsolutePath());
        return ensureDirectory(directoryName);
    }

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), key);
    }

    private static class FileAndTarget { // TODO same as in BRouter
        public final File file;
        public final java.io.File target;

        private FileAndTarget(File file, java.io.File target) {
            this.file = file;
            this.target = target;
        }
    }

    public void downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<String> keys = new HashSet<String>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add("europe_germany.ghz"); // TODO too simple
        }

        Set<FileAndTarget> files = new HashSet<FileAndTarget>();
        for (String key : keys) {
            File catalog = fileMap.get(key);
            if (catalog != null)
                files.add(new FileAndTarget(catalog, createFile(key)));
        }

        Collection<Download> downloads = new HashSet<Download>();
        for (FileAndTarget file : files) {
            if (new Validator(file.target).existsFile())
                continue;
            downloads.add(download(file));
        }

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private Download download(FileAndTarget file) {
        String uri = file.file.getUri();
        String url = getBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " routing data for " + uri, url,
                file.file.getSize(), file.file.getChecksum(), Copy, file.target);
    }
}
