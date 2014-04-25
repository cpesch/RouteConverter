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
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.actions.Validator;
import slash.navigation.download.datasources.DataSourceService;
import slash.navigation.download.datasources.File;
import slash.navigation.download.helpers.FileAndTarget;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.graphhopper.util.CmdArgs.read;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.removeExtension;
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

    private final DownloadManager downloadManager;
    private com.graphhopper.GraphHopper hopper;
    private Map<String, File> fileMap;
    private String baseUrl, directory;
    private java.io.File osmPbfFile = null;

    public GraphHopper(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        initialize();
    }

    private void initialize() {
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

    public String getName() {
        return "GraphHopper";
    }

    private String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE, baseUrl);
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isSupportTurnpoints() {
        return false;
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to) {
        if(osmPbfFile == null)
            return null;
        initializeHopper();

        GHRequest request = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
        request.setVehicle("car"); // TODO make configurable
        GHResponse response = hopper.route(request);
        return new RoutingResult(asPositions(response.getPoints()), response.getDistance(), response.getMillis());
    }

    private void initializeHopper() {
        if (hopper != null && hopper.getGraphHopperLocation().equals(createPath(osmPbfFile)))
            return;

        String[] args = new String[]{
                "graph.location=" + createPath(osmPbfFile),
                "osmreader.osm=" + osmPbfFile.getAbsolutePath()
        };
        try {
            if (hopper != null)
                hopper.close();
            hopper = new com.graphhopper.GraphHopper().forDesktop();
            hopper.init(read(args));
            hopper.importOrLoad();
        } catch (Exception e) {
            log.warning("Cannot initialize GraphHopper: " + e.getMessage());
        }
    }

    private String createPath(java.io.File osmPbfFile) {
        return removeExtension(removeExtension(osmPbfFile.getAbsolutePath()));
    }

    private List<NavigationPosition> asPositions(PointList points) {
        List<NavigationPosition> result = new ArrayList<NavigationPosition>();
        for (int i = 0, c = points.getSize(); i < c; i++) {
            result.add(new SimpleNavigationPosition(points.getLongitude(i), points.getLatitude(i)));
        }
        return result;
    }

    private java.io.File getDirectory() {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if (!f.exists())
            directoryName = getApplicationDirectory(directory).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        BoundingBox routeBoundingBox = createBoundingBox(longitudeAndLatitudes);

        String keyForSmallestBoundingBox = null;
        BoundingBox smallestBoundingBox = null;
        for (String key : fileMap.keySet()) {
            File file = fileMap.get(key);
            BoundingBox fileBoundingBox = file.getBoundingBox();
            if (fileBoundingBox.contains(routeBoundingBox)) {
                if (smallestBoundingBox == null || smallestBoundingBox.contains(fileBoundingBox)) {
                    keyForSmallestBoundingBox = key;
                    smallestBoundingBox = fileBoundingBox;
                }
            }
        }

        final FileAndTarget fileAndTarget = createFileAndTarget(keyForSmallestBoundingBox);
        this.osmPbfFile = fileAndTarget != null ? fileAndTarget.target : null;

        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return fileAndTarget != null && !new Validator(fileAndTarget.target).existsFile();
            }

            public void download() {
                if (fileAndTarget != null)
                    downloadFile(fileAndTarget);
            }
        };
    }

    private BoundingBox createBoundingBox(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            positions.add(new SimpleNavigationPosition(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }
        return new BoundingBox(positions);
    }

    private FileAndTarget createFileAndTarget(String keyForSmallestBoundingBox) {
        if (keyForSmallestBoundingBox != null) {
            File catalog = fileMap.get(keyForSmallestBoundingBox);
            if (catalog != null) {
                java.io.File file = createFile(keyForSmallestBoundingBox);
                return new FileAndTarget(catalog, file);
            }
        }
        return null;
    }

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), key);
    }

    private void downloadFile(FileAndTarget retrieve) {
        Download download = initiateDownload(retrieve);
        downloadManager.waitForCompletion(asList(download));
    }

    private Download initiateDownload(FileAndTarget file) {
        String uri = file.file.getUri();
        String url = getBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " routing data for " + uri, url,
                file.file.getSize(), file.file.getChecksum(), file.file.getTimestamp(), Copy, file.target);
    }

}
