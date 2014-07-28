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
import slash.navigation.datasources.DataSource;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.graphhopper.util.CmdArgs.read;
import static java.util.Arrays.asList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.removeExtension;

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

    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("Bike"), new TravelMode("Car"),
            new TravelMode("Foot"));

    private final DataSource dataSource;
    private final DownloadManager downloadManager;
    private com.graphhopper.GraphHopper hopper;
    private java.io.File osmPbfFile = null;

    public GraphHopper(DataSource dataSource, DownloadManager downloadManager) {
        this.dataSource = dataSource;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return dataSource.getName();
    }

    private String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE, dataSource.getBaseUrl());
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isSupportTurnpoints() {
        return false;
    }

    public List<TravelMode> getAvailableTravelModes() {
        return TRAVEL_MODES;
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    private java.io.File getDirectory() {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if (!f.exists())
            directoryName = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        if(osmPbfFile == null)
            return null;


        GHRequest request = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
        request.setVehicle(travelMode.getName().toUpperCase());
        GHResponse response = hopper.route(request);
        return new RoutingResult(asPositions(response.getPoints()), response.getDistance(), response.getMillis());
    }

    private String getAvailableTravelModeNames() {
        StringBuilder result = new StringBuilder();
        List<TravelMode> availableTravelModes = getAvailableTravelModes();
        for(int i=0; i < availableTravelModes.size(); i++) {
            result.append(availableTravelModes.get(i).getName().toUpperCase());
            if(i < availableTravelModes.size() - 1)
                result.append(",");
        }
        return result.toString();
    }

    void initializeHopper(java.io.File osmPbfFile) {
        this.osmPbfFile = osmPbfFile;
        String[] args = new String[]{
                "prepare.doPrepare=false",
                "prepare.chShortcuts=false",
                "graph.location=" + createPath(osmPbfFile),
                "osmreader.acceptWay=" + getAvailableTravelModeNames(),
                "osmreader.osm=" + osmPbfFile.getAbsolutePath()
        };
        try {
            if (hopper != null)
                hopper.close();
            hopper = new com.graphhopper.GraphHopper().forDesktop();
            hopper.init(read(args));
            hopper.importOrLoad();
        } catch (Exception e) {
            log.warning("Cannot initialize GraphHopper: " + e);
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

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        BoundingBox routeBoundingBox = createBoundingBox(longitudeAndLatitudes);
        String keyForSmallestBoundingBox = null;
        BoundingBox smallestBoundingBox = null;
        /* TODO
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
        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return fileAndTarget != null && !new Validator(fileAndTarget.target).existsFile();
            }
            public boolean isRequiresProcessing() {
                return true;
            }
            public void download() {
                downloadFile(fileAndTarget);
            }
            public void process() {
                initializeHopper(fileAndTarget);
            }
        };
        */
        return null;
    }

    private BoundingBox createBoundingBox(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            positions.add(new SimpleNavigationPosition(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }
        return new BoundingBox(positions);
    }

    /* TODO
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
    */

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), key);
    }

    /* TODO
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
    */
}
