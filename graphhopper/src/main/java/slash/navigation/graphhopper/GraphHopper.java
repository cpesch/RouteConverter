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
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.PointList;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.common.io.Files.recursiveDelete;
import static slash.navigation.graphhopper.PbfUtil.DOT_OSM;
import static slash.navigation.graphhopper.PbfUtil.DOT_PBF;

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
    // omitted: Hike, MotorCycle, MTB, RacingBike, Scooter, Small_Truck, Truck
    private static final int BYTES_FOR_EDGE_FLAGS = 4;

    private final DownloadManager downloadManager;
    private DataSource dataSource;

    private DownloadableFinder finder;
    private com.graphhopper.GraphHopper hopper;
    private java.io.File osmPbfFile;

    public GraphHopper(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return "GraphHopper";
    }

    public synchronized boolean isInitialized() {
        return getDataSource() != null;
    }

    public synchronized DataSource getDataSource() {
        return dataSource;
    }

    public synchronized void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        finder = new DownloadableFinder(dataSource, getDirectory());
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isSupportTurnpoints() {
        return false;
    }

    public boolean isSupportAvoidFerries() {
        return false;
    }

    public boolean isSupportAvoidHighways() {
        return false;
    }

    public boolean isSupportAvoidTolls() {
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
        return preferences.get(BASE_URL_PREFERENCE, getDataSource().getBaseUrl());
    }

    private java.io.File getDirectory() {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if (!f.exists())
            directoryName = getApplicationDirectory(getDataSource().getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), key);
    }

    private java.io.File createPath(java.io.File file) {
        String name = file.getName().replace(DOT_PBF, "").replace(DOT_OSM, "");
        return new java.io.File(file.getParent(), name);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        initializeHopper();

        long start = currentTimeMillis();
        try {
            GHRequest request = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
            request.setVehicle(travelMode.getName().toUpperCase());
            GHResponse response = hopper.route(request);
            if(response.hasErrors()) {
                String errors = printArrayToDialogString(response.getErrors().toArray(), false);
                log.severe(format("Error while routing between %s and %s: %s", from, to, errors));
                throw new RuntimeException(errors);
            }
            PathWrapper best = response.getBest();
            return new RoutingResult(asPositions(best.getPoints()), new DistanceAndTime(best.getDistance(), best.getTime() / 1000), true);
        } finally {
            long end = currentTimeMillis();
            log.info("GraphHopper: routing from " + from + " to " + to + " took " + (end - start) + " milliseconds");
        }
    }

    private String getAvailableTravelModeNames() {
        StringBuilder result = new StringBuilder();
        List<TravelMode> availableTravelModes = getAvailableTravelModes();
        for(int i=0; i < availableTravelModes.size(); i++) {
            result.append(availableTravelModes.get(i).getName().toLowerCase());
            if(i < availableTravelModes.size() - 1)
                result.append(",");
        }
        return result.toString();
    }

    private static final Object initializationLock = new Object();

    private java.io.File getOsmPbfFile() {
        synchronized (initializationLock) {
            return osmPbfFile;
        }
    }

    void setOsmPbfFile(java.io.File osmPbfFile) {
        synchronized (initializationLock) {
            this.osmPbfFile = osmPbfFile;
        }
    }

    void initializeHopper() {
        synchronized (initializationLock) {
            java.io.File file = getOsmPbfFile();
            if (file == null)
                return;

            if (hopper != null) {
                // avoid close() and importOrLoad() if the osmPbfFile stayed the same
                if (((GraphHopperOSM)hopper).getOSMFile().equals(file.getAbsolutePath()))
                    return;

                hopper.close();
            }

            File path = createPath(file);
            try {
                hopper = new GraphHopperOSM().
                        setOSMFile(file.getAbsolutePath()).
                        forDesktop().
                        setEncodingManager(new EncodingManager(getAvailableTravelModeNames(), BYTES_FOR_EDGE_FLAGS)).
                        setCHEnabled(false).
                        setEnableInstructions(false).
                        setGraphHopperLocation(path.getAbsolutePath()).
                        importOrLoad();
            } catch (IllegalStateException e) {
                log.warning("Could not initialize GraphHopper: " + e);

                if (e.getMessage().contains("Version of nodes unsupported") || e.getMessage().contains("Encoding does not match")) {
                    log.info("Deleting GraphHopper index " + path);
                    try {
                        recursiveDelete(path);
                        log.info("Reinitializing GraphHopper");
                        initializeHopper();
                    } catch (IOException e2) {
                        log.severe("Could not delete GraphHopper index " + path + ": " + e2);
                    }
                }

                throw e;
            }

            setOsmPbfFile(null);
        }
    }

    private List<NavigationPosition> asPositions(PointList points) {
        List<NavigationPosition> result = new ArrayList<>();
        for (int i = 0, c = points.getSize(); i < c; i++) {
            result.add(new SimpleNavigationPosition(points.getLongitude(i), points.getLatitude(i), points.getElevation(i), null));
        }
        return result;
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        BoundingBox routeBoundingBox = createBoundingBox(longitudeAndLatitudes);
        final Downloadable downloadable = finder.getDownloadableFor(routeBoundingBox);
        final java.io.File file = downloadable != null ? createFile(downloadable.getUri()) : null;
        setOsmPbfFile(file);

        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return file != null && !file.exists();
            }
            public boolean isRequiresProcessing() {
                if(file == null)
                    return false;

                File path = createPath(file);
                File edges = new File(path, "edges");
                return !path.exists() || !edges.exists();
            }
            public void download() {
                downloadAndWait(downloadable);
            }
            public void process() {
                initializeHopper();
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

    private void downloadAndWait(Downloadable downloadable) {
        Download download = download(downloadable);
        downloadManager.waitForCompletion(singletonList(download));
    }

    private Download download(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " Routing Data: " + uri, url, Action.valueOf(dataSource.getAction()),
                new FileAndChecksum(createFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }

    public long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> downloadables = finder.getDownloadableFor(boundingBoxes);
        long notExists = 0L;
        for(Downloadable downloadable : downloadables) {
            Long contentLength = downloadable.getLatestChecksum().getContentLength();
            if(contentLength == null)
                continue;

            java.io.File file = createFile(downloadable.getUri());
            if(!file.exists())
                notExists += contentLength;
        }
        return notExists;
    }

    public void downloadRoutingData(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> downloadables = finder.getDownloadableFor(boundingBoxes);
        for (Downloadable downloadable : downloadables) {
            download(downloadable);
        }
    }
}
