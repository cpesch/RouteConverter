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
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.File;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.graphhopper.util.CmdArgs.read;
import static java.lang.System.currentTimeMillis;
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
    private static final TravelMode CAR = new TravelMode("Car");
    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("Bike"), CAR, new TravelMode("Foot"));

    private final DataSource dataSource;
    private final DownloadManager downloadManager;

    private com.graphhopper.GraphHopper hopper;
    private java.io.File osmPbfFile = null;

    public GraphHopper(DataSource dataSource, DownloadManager downloadManager) {
        this.dataSource = dataSource;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return "GraphHopper";
    }

    public synchronized boolean isInitialized() {
        return dataSource != null && downloadManager != null;
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

    public TravelMode getPreferredTravelMode() {
        return CAR;
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    private String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE, dataSource.getBaseUrl());
    }

    private java.io.File getDirectory() {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if (!f.exists())
            directoryName = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), key);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        if(osmPbfFile == null)
            return null;

        long start = currentTimeMillis();
        try {
            GHRequest request = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
            request.setVehicle(travelMode.getName().toUpperCase());
            GHResponse response = hopper.route(request);
            return new RoutingResult(asPositions(response.getPoints()), response.getDistance(), response.getMillis(), true);
        } finally {
            long end = currentTimeMillis();
            log.fine(getClass().getSimpleName() + ": routing took " + (end - start) + " milliseconds");
        }
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
        List<NavigationPosition> result = new ArrayList<>();
        for (int i = 0, c = points.getSize(); i < c; i++) {
            result.add(new SimpleNavigationPosition(points.getLongitude(i), points.getLatitude(i)));
        }
        return result;
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        BoundingBox routeBoundingBox = createBoundingBox(longitudeAndLatitudes);
        Downloadable downloadableForSmallestBoundingBox = null;
        BoundingBox smallestBoundingBox = null;

        for (File file : dataSource.getFiles()) {
            BoundingBox fileBoundingBox = file.getBoundingBox();
            if (fileBoundingBox != null && fileBoundingBox.contains(routeBoundingBox)) {
                if (smallestBoundingBox == null || smallestBoundingBox.contains(fileBoundingBox)) {
                    downloadableForSmallestBoundingBox = file;
                    smallestBoundingBox = fileBoundingBox;
                }
            }
        }

        final Downloadable downloadable = downloadableForSmallestBoundingBox;
        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return downloadable != null && !createFile(downloadable.getUri()).exists();
            }
            public boolean isRequiresProcessing() {
                return true;
            }
            public void download() {
                downloadAll(downloadable);
            }
            public void process() {
                if(downloadable != null)
                    initializeHopper(createFile(downloadable.getUri()));
            }
        };
    }

    private BoundingBox createBoundingBox(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        List<NavigationPosition> positions = new ArrayList<>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            positions.add(new SimpleNavigationPosition(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }
        return new BoundingBox(positions);
    }

    private void downloadAll(Downloadable downloadable) {
        Download download = download(downloadable);
        downloadManager.waitForCompletion(asList(download));
    }

    private Download download(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " Routing Data: " + uri, url, Copy,
                null, new FileAndChecksum(createFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }
}
