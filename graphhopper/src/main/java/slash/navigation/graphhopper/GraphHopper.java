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
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PointList;
import com.graphhopper.util.exceptions.DetailedIllegalArgumentException;
import com.graphhopper.util.shapes.GHPoint3D;
import slash.common.io.Files;
import slash.navigation.common.*;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.routing.*;
import slash.navigation.routing.RoutingResult.Validity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.graphhopper.json.Statement.If;
import static com.graphhopper.json.Statement.Op.LIMIT;
import static com.graphhopper.json.Statement.Op.MULTIPLY;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.*;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Files.asDialogString;
import static slash.common.io.Files.removeExtension;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.graphhopper.PbfUtil.lookupGraphDirectory;
import static slash.navigation.routing.RoutingResult.Validity.*;

/**
 * Encapsulates access to the GraphHopper.
 *
 * @author Christian Pesch
 */

public class GraphHopper extends BaseRoutingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(GraphHopper.class);
    private static final Logger log = Logger.getLogger(GraphHopper.class.getName());
    private static final String BASE_URL_PREFERENCE = "baseUrl";
    private static final TravelMode CAR = new TravelMode("car");
    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("bike"), CAR, new TravelMode("foot"));
    static boolean TEST_MODE = false;

    private final DownloadManager downloadManager;
    private GraphManager graphManager;

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
        return graphManager != null;
    }

    public synchronized void setDataSources(DataSource kurviger, DataSource mapsforge, DataSource graphHopper) throws IOException {
        this.graphManager = new GraphManager(kurviger, mapsforge, graphHopper);
        this.finder = new DownloadableFinder(graphManager);
    }

    public boolean isDownload() {
        return true;
    }

    public List<TravelMode> getAvailableTravelModes() {
        return TRAVEL_MODES;
    }

    public TravelMode getPreferredTravelMode() {
        return CAR;
    }

    public TravelRestrictions getAvailableTravelRestrictions() {
        return new TravelRestrictions(true, true, true, false, true);
    }

    public String getPath() {
        return graphManager != null ? graphManager.getPath() : "";
    }

    public void setPath(String path) {
        graphManager.setPath(path);
    }

    private String getBaseUrl(DataSource dataSource) {
        return preferences.get(BASE_URL_PREFERENCE + dataSource.getName(), dataSource.getBaseUrl());
    }

    private java.io.File getDirectory(DataSource dataSource) {
        return graphManager.getDirectory(dataSource);
    }

    private java.io.File createFile(Downloadable downloadable) {
        return new java.io.File(getDirectory(downloadable.getDataSource()), downloadable.getUri());
    }

    private java.io.File createFile(GraphDescriptor graphDescriptor) {
        return graphDescriptor.getLocalFile() != null ? graphDescriptor.getLocalFile() :
                new java.io.File(getDirectory(graphDescriptor.getRemoteFile().getDataSource()), graphDescriptor.getRemoteFile().getUri());
    }

    private java.io.File createDirectory(Downloadable downloadable) {
        return ensureDirectory(new java.io.File(getDirectory(downloadable.getDataSource()), removeExtension(downloadable.getUri())).getParentFile());
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode, TravelRestrictions travelRestrictions) {
        initializeHopper();
        if (hopper == null)
            throw new IllegalStateException("Could not initialize from graph directory of GraphHopper");

        SecondCounter counter = new SecondCounter() {
            protected void second(int second) {
                fireRouting(second);
            }
        };
        counter.start();

        long start = currentTimeMillis();
        try {
            GHRequest request = new GHRequest(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
            request.setProfile(travelMode.getName());
            CustomModel customModel = new CustomModel();
            if (travelRestrictions.isAvoidBridges())
                customModel.addToPriority(If("road_environment == BRIDGE", MULTIPLY, "0"));
            if (travelRestrictions.isAvoidFerries())
                customModel.addToPriority(If("road_environment == FERRY", MULTIPLY, "0"));
            if (travelRestrictions.isAvoidMotorways())
                customModel.addToPriority(If("road_class == MOTORWAY", MULTIPLY, "0"));
            // if (travelRestrictions.isAvoidToll())
            //    customModel.addToPriority(Statement.If("toll == all", MULTIPLY, "0"));
            if (travelRestrictions.isAvoidTunnels())
                customModel.addToPriority(If("road_environment == TUNNEL", MULTIPLY, "0"));
            request.setCustomModel(customModel);
            GHResponse response = hopper.route(request);
            if (response.hasErrors()) {
                String errors = asDialogString(response.getErrors(), false);
                log.severe(format("Error while routing between %s and %s: %s", from, to, errors));

                boolean pointNotFound = !response.getErrors().isEmpty() && response.getErrors().get(0) instanceof DetailedIllegalArgumentException;
                if (pointNotFound)
                    return new RoutingResult(null, null, PointNotFound);

                throw new RuntimeException(errors);
            }
            ResponsePath path = response.getBest();
            Validity validity = path.getErrors().isEmpty() ? Valid : Invalid;
            return new RoutingResult(asPositions(path.getPoints()), new DistanceAndTime(path.getDistance(), path.getTime()), validity);
        } finally {
            counter.stop();

            long end = currentTimeMillis();
            log.info(format("Routing from %s to %s with %s took %d milliseconds", from, to, getOsmPbfFile(), end - start));
        }
    }

    private synchronized java.io.File getOsmPbfFile() {
        return osmPbfFile;
    }

    synchronized void setOsmPbfFile(java.io.File osmPbfFile) {
        this.osmPbfFile = osmPbfFile;
    }

    private boolean existsOsmPbfFile() {
        File file = getOsmPbfFile();
        return file != null && file.exists();
    }

    private File getGraphDirectory() {
        File file = getOsmPbfFile();
        return file != null ? lookupGraphDirectory(file) : null;
    }

    private boolean existsGraphDirectory() {
        File graphDirectory = getGraphDirectory();
        return graphDirectory != null && PbfUtil.createPropertiesFile(graphDirectory).exists();
    }

    synchronized void initializeHopper() {
        if (!existsGraphDirectory() && !existsOsmPbfFile())
            return;

        java.io.File osmPbfFile = getOsmPbfFile();
        File graphDirectory = getGraphDirectory();
        if (hopper != null) {
            // avoid close() and importOrLoad() if the osmPbfFile stayed the same
            String location = hopper.getGraphHopperLocation();
            if (location != null && graphDirectory != null && location.equals(graphDirectory.getAbsolutePath()))
                return;

            hopper.close();
            hopper = null;
        }

        SecondCounter counter = new SecondCounter() {
            protected void second(int second) {
                fireInitializing(second);
            }
        };
        counter.start();

        long start = currentTimeMillis();
        try {
            // load existing graph first
            if (existsGraphDirectory()) {
                log.info(format("Loading existing graph from %s", graphDirectory));
                this.hopper = graphDirectory != null ? loadHopper(graphDirectory) : null;
                if (this.hopper != null)
                    return;
            }

            // if there is none or it fails:
            log.info(format("Creating graph from %s to %s", osmPbfFile, graphDirectory));
            this.hopper = graphDirectory != null ? importHopper(osmPbfFile, graphDirectory) : null;
        } catch (IllegalStateException e) {
            log.warning("Could not initialize GraphHopper: " + e);
            throw e;
        } finally {
            counter.stop();

            long end = currentTimeMillis();
            log.info(format("Initializing from %s took %d milliseconds", graphDirectory, end - start));
        }
    }

    private com.graphhopper.GraphHopper createHopper() {
        List<Profile> profiles = getAvailableTravelModes().stream()
                .map(mode -> new Profile(mode.getName())
                                .setName(mode.getName())
                                .setCustomModel(new CustomModel()
                                        .addToPriority(If("!" + mode.getName() + "_access", MULTIPLY, "0"))
                                        .addToSpeed(If("true", LIMIT, mode.getName() + "_average_speed")))
                )
                .toList();

        String modePriorityAndSpeed = getAvailableTravelModes().stream()
                .flatMap(mode -> Stream.of(mode.getName() + "_access", mode.getName() + "_average_speed"))
                .collect(Collectors.joining(","));

        return new com.graphhopper.GraphHopper()
                .setEncodedValuesString("road_class,road_environment,toll," + modePriorityAndSpeed)
                .setProfiles(profiles);
    }

    private com.graphhopper.GraphHopper loadHopper(File graphDirectory) {
        com.graphhopper.GraphHopper result = createHopper()
                .setGraphHopperLocation(graphDirectory.getAbsolutePath());
        try {
            if (result.load())
                return result;
        } catch (IllegalStateException e) {
            log.warning(format("GraphHopper couldn't read %s: %s. Deleting then reimporting.", graphDirectory, e.getLocalizedMessage()));

            try {
                Files.recursiveDelete(graphDirectory);
                log.warning(format("Deleted %s. Now reimporting", graphDirectory));
            } catch (IOException ex) {
                log.warning(format("RouteConverter couldn't delete %s: %s. Failing.", graphDirectory, e.getLocalizedMessage()));
            }
        }
        return null;
    }

    private com.graphhopper.GraphHopper importHopper(File osmPbfFile, File graphDirectory) {
        com.graphhopper.GraphHopper result = createHopper();
        result.setOSMFile(osmPbfFile.getAbsolutePath())
                // could set .setElevation(true) and .setElevationProvider(...)
                .setGraphHopperLocation(graphDirectory.getAbsolutePath());
        return result.importOrLoad();
    }

    private List<NavigationPosition> asPositions(PointList points) {
        List<NavigationPosition> result = new ArrayList<>();
        for (int i = 0, c = points.size(); i < c; i++) {
            GHPoint3D ghPoint = points.get(i);
            result.add(new SimpleNavigationPosition(ghPoint.getLon(), ghPoint.getLat(), ghPoint.getEle(), null));
        }
        return result;
    }

    public DownloadFuture downloadRoutingDataFor(String mapIdentifier, List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<MapDescriptor> mapDescriptors = new HashSet<>();
        for (int i = 0; i < longitudeAndLatitudes.size() - 1; i += 2) {
            LongitudeAndLatitude l1 = longitudeAndLatitudes.get(i);
            LongitudeAndLatitude l2 = longitudeAndLatitudes.get(i + 1);
            mapDescriptors.add(new LatitudeAndLongitudeMapDescriptor(mapIdentifier, l1, l2));
        }

        List<GraphDescriptor> graphDescriptors = finder.getGraphDescriptorsFor(mapDescriptors);
        return new DownloadFutureImpl(graphDescriptors);
    }

    private class DownloadFutureImpl implements DownloadFuture {
        private final List<GraphDescriptor> graphDescriptors;
        private GraphDescriptor next;

        DownloadFutureImpl(Collection<GraphDescriptor> graphDescriptors) {
            this.graphDescriptors = new ArrayList<>(graphDescriptors);
            this.next = !graphDescriptors.isEmpty() ? this.graphDescriptors.remove(0) : null;
        }

        public boolean isRequiresDownload() {
            boolean requiresDownload = !existsOsmPbfFile() && !existsGraphDirectory();
            if (requiresDownload)
                log.fine("requiresDownload=" + requiresDownload +
                        " existsGraphDirectory()=" + existsGraphDirectory() + " getGraphDirectory()=" + getGraphDirectory() +
                        " existsOsmPbfFile()=" + existsOsmPbfFile() + " getOsmPbfFile()=" + getOsmPbfFile() +
                        " graphDescriptors=" + graphDescriptors);
            return requiresDownload && confirmDownload();
        }

        private boolean confirmDownload() {
            while(!graphDescriptors.isEmpty()) {
                slash.navigation.datasources.File file = next.getRemoteFile();
                if (file == null || TEST_MODE)
                    return true;

                Long size = file.getLatestChecksum() != null ? file.getLatestChecksum().getContentLength() : null;
                int confirm = showConfirmDialog(null,
                        "Do you want to download the routing data\n" +
                                file.getUri() + "\n" +
                                "with a size of " + (size != null ? size / (1024 * 1024) : "a large number of ") + " MBytes?",
                        "GraphHopper", YES_NO_OPTION);
                if (confirm == YES_OPTION)
                    return true;
                this.next = !graphDescriptors.isEmpty() ? graphDescriptors.remove(0) : null;
            }
            if (next != null)
                setOsmPbfFile(createFile(next));
            return false;
        }

        public void download() {
            fireDownloading();
            downloadAndWait(next);
            setOsmPbfFile(createFile(next));
        }

        public boolean isRequiresProcessing() {
            boolean requiresProcessing = !existsGraphDirectory() && existsOsmPbfFile();
            if (requiresProcessing)
                log.info("requiresProcessing=" + requiresProcessing +
                        " existsGraphDirectory()=" + existsGraphDirectory() + " getGraphDirectory()=" + getGraphDirectory() +
                        " existsOsmPbfFile()=" + existsOsmPbfFile() + " getOsmPbfFile()=" + getOsmPbfFile());
            return requiresProcessing;
        }

        public void process() {
            initializeHopper();
        }
    }

    private void downloadAndWait(GraphDescriptor graphDescriptor) {
        Downloadable downloadable = graphDescriptor.getRemoteFile();
        if (downloadable != null) {
            Download download = download(downloadable);
            downloadManager.waitForCompletion(singletonList(download));
        }
    }

    private void download(GraphDescriptor graphDescriptor) {
        Downloadable downloadable = graphDescriptor.getRemoteFile();
        if (downloadable != null) {
            download(downloadable);
        }
    }

    private Download download(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getBaseUrl(downloadable.getDataSource()) + uri;
        Action action = Action.valueOf(downloadable.getDataSource().getAction());
        File file = action.equals(Extract) ? createDirectory(downloadable) : createFile(downloadable);
        return downloadManager.queueForDownload(getName() + " Routing Data: " + uri, url, action,
                new FileAndChecksum(file, downloadable.getLatestChecksum()), null);
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        List<GraphDescriptor> graphDescriptors = finder.getGraphDescriptorsFor(mapDescriptors);
        long notExists = 0L;
        for (GraphDescriptor graphDescriptor : graphDescriptors) {
            Downloadable downloadable = graphDescriptor.getRemoteFile();
            if (downloadable == null)
                continue;

            Long contentLength = downloadable.getLatestChecksum() != null ? downloadable.getLatestChecksum().getContentLength() : null;
            if (contentLength == null)
                continue;

            java.io.File file = createFile(downloadable);
            if (!file.exists()) {
                notExists += contentLength;
                break;
            }
        }
        return notExists;
    }

    public void downloadRoutingData(List<MapDescriptor> mapDescriptors) {
        List<GraphDescriptor> graphDescriptors = finder.getGraphDescriptorsFor(mapDescriptors);
        for (GraphDescriptor graphDescriptor : graphDescriptors) {
            download(graphDescriptor);
            // avoid multiple downloads when kurviger, mapsforge graphs and geofabrik PBFs are returned
            break;
        }
    }
}
