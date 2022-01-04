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
import com.graphhopper.routing.util.DefaultFlagEncoderFactory;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.PointList;
import com.graphhopper.util.exceptions.PointNotFoundException;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.graphhopper.routing.ch.CHAlgoFactoryDecorator.EdgeBasedCHMode.EDGE_OR_NODE;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.asDialogString;
import static slash.common.io.Files.removeExtension;
import static slash.common.io.Transfer.trim;
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
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";
    private static final TravelMode CAR = new TravelMode("car");
    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("bike"), CAR, new TravelMode("foot"));

    private final DownloadManager downloadManager;
    private List<DataSource> dataSources;

    private DownloadableFinder finder;
    private com.graphhopper.GraphHopper hopper;
    private final EncodingManager encodingManager;
    private java.io.File osmPbfFile;

    public GraphHopper(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;

        // disable options to reduce graph creation times
        /*
        EncodedValueFactory encodedValueFactory = new DefaultEncodedValueFactory();
        this.encodingManager = new EncodingManager
                .Builder(8)
                .setEnableInstructions(true)
                .add(encodedValueFactory.create("road_class"))
                .add(encodedValueFactory.create("road_class_link"))
                .add(encodedValueFactory.create("road_environment"))
                .add(encodedValueFactory.create("max_speed"))
                .add(encodedValueFactory.create("road_access"))
                .add(new CarFlagEncoder("turn_costs=true|edge_based=true"))
                .add(new FootFlagEncoder())
                .add(new BikeFlagEncoder())
                .build();
        */
        this.encodingManager = new EncodingManager
                .Builder(4)
                .setEnableInstructions(false)
                .addAll(new DefaultFlagEncoderFactory(), getAvailableTravelModeNames()).
                build();
    }

    public String getName() {
        return "GraphHopper";
    }

    public synchronized boolean isInitialized() {
        return dataSources != null;
    }

    public synchronized void setDataSources(DataSource... dataSourcesList) {
        this.dataSources = asList(dataSourcesList);
        finder = new DownloadableFinder(dataSources);
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

    private String getAvailableTravelModeNames() {
        StringBuilder result = new StringBuilder();
        List<TravelMode> availableTravelModes = getAvailableTravelModes();
        for (int i = 0; i < availableTravelModes.size(); i++) {
            result.append(availableTravelModes.get(i).getName().toLowerCase());
            if (i < availableTravelModes.size() - 1)
                result.append(",");
        }
        return result.toString();
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    private String getBaseUrl(DataSource dataSource) {
        return preferences.get(BASE_URL_PREFERENCE + dataSource.getName(), dataSource.getBaseUrl());
    }

    private java.io.File getDirectory(DataSource dataSource) {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if (!f.exists())
            directoryName = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    private java.io.File createFile(Downloadable downloadable) {
        return new java.io.File(getDirectory(downloadable.getDataSource()), downloadable.getUri());
    }

    private java.io.File createDirectory(Downloadable downloadable) {
        return ensureDirectory(new java.io.File(getDirectory(downloadable.getDataSource()), removeExtension(downloadable.getUri())).getParentFile());
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
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
            request.setVehicle(travelMode.getName().toUpperCase());
            GHResponse response = hopper.route(request);
            if (response.hasErrors()) {
                boolean pointNotFound = response.getErrors().size() > 0 && response.getErrors().get(0) instanceof PointNotFoundException;
                if (pointNotFound)
                    return new RoutingResult(null, null, PointNotFound);

                String errors = asDialogString(response.getErrors(), false);
                log.severe(format("Error while routing between %s and %s: %s", from, to, errors));
                throw new RuntimeException(errors);
            }
            PathWrapper best = response.getBest();
            Validity validity = best.getErrors().size() == 0 ? Valid : Invalid;
            return new RoutingResult(asPositions(best.getPoints()), new DistanceAndTime(best.getDistance(), best.getTime()), validity);
        } finally {
            counter.stop();

            long end = currentTimeMillis();
            log.info(format("Routing from %s to %s took %d milliseconds", from, to, end - start));
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
        return lookupGraphDirectory(file);
    }

    private boolean existsGraphDirectory() {
        return PbfUtil.createPropertiesFile(getGraphDirectory()).exists();
    }

    synchronized void initializeHopper() {
        if (!existsGraphDirectory() && !existsOsmPbfFile())
            return;

        java.io.File osmPbfFile = getOsmPbfFile();
        File graphDirectory = getGraphDirectory();
        if (hopper != null) {
            // avoid close() and importOrLoad() if the osmPbfFile stayed the same
            String location = hopper.getGraphHopperLocation();
            if (location != null && location.equals(graphDirectory.getAbsolutePath()))
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
                this.hopper = loadHopper(graphDirectory);
                if (this.hopper != null)
                    return;
            }

            // if there is none or it fails:
            log.info(format("Creating graph from %s to %s", osmPbfFile, graphDirectory));
            this.hopper = importHopper(osmPbfFile, graphDirectory);
        } catch (IllegalStateException e) {
            log.warning("Could not initialize GraphHopper: " + e);
            throw e;
        } finally {
            counter.stop();

            long end = currentTimeMillis();
            log.info(format("Initializing from %s took %d milliseconds", graphDirectory, end - start));
        }
    }

    private com.graphhopper.GraphHopper loadHopper(File graphDirectory) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(PbfUtil.createPropertiesFile(graphDirectory)));
        } catch (IOException e) {
            log.warning(format("Cannot load properties: %s", e.getMessage()));
        }

        GraphHopperOSM osm = new GraphHopperOSM();
        boolean existsCH = !"[]".equals(trim(properties.getProperty("graph.ch.profiles")));
        osm.getCHFactoryDecorator().setEnabled(existsCH);
        if(existsCH)
            osm.getCHFactoryDecorator().setEdgeBasedCHMode(EDGE_OR_NODE);

        if(osm.load(graphDirectory.getAbsolutePath()))
            return osm;
        else
            return null;
    }

    private com.graphhopper.GraphHopper importHopper(File osmPbfFile, File graphDirectory) {
        GraphHopperOSM osm = new GraphHopperOSM()
                .setOSMFile(osmPbfFile.getAbsolutePath());
        // disable options to reduce graph creation times
        // osm.getCHFactoryDecorator().setEdgeBasedCHMode(EDGE_OR_NODE);
        return osm
                .setEncodingManager(encodingManager)
                .setGraphHopperLocation(graphDirectory.getAbsolutePath())
                .forDesktop()
                .setCHEnabled(false) // disabled
                .importOrLoad();
    }

    private List<NavigationPosition> asPositions(PointList points) {
        List<NavigationPosition> result = new ArrayList<>();
        for (int i = 0, c = points.getSize(); i < c; i++) {
            result.add(new SimpleNavigationPosition(points.getLongitude(i), points.getLatitude(i), points.getElevation(i), null));
        }
        return result;
    }

    public DownloadFuture downloadRoutingDataFor(String mapIdentifier, List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<MapDescriptor> mapDescriptors = new HashSet<>();
        for (int i = 0; i < longitudeAndLatitudes.size() - 1; i += 2) {
            LongitudeAndLatitude l1 = longitudeAndLatitudes.get(i);
            LongitudeAndLatitude l2 = longitudeAndLatitudes.get(i + 1);
            mapDescriptors.add(new MapDescriptor() {
                public String getIdentifier() {
                    return mapIdentifier;
                }
                public BoundingBox getBoundingBox() {
                    return createBoundingBox(asList(l1, l2));
                }
                public String toString() {
                    return "MapDescriptor[identifier=" + getIdentifier() + ", boundingBox=" + getBoundingBox() + "]";
                }
            });
        }

        Collection<Downloadable> downloadables = finder.getDownloadablesFor(mapDescriptors);
        return new DownloadFutureImpl(downloadables);
    }

    private class DownloadFutureImpl implements DownloadFuture {
        private final List<Downloadable> downloadables;
        private Downloadable next;

        DownloadFutureImpl(Collection<Downloadable> downloadables) {
            this.downloadables = new ArrayList<>(downloadables);
            nextDownload();
        }

        public boolean isRequiresDownload() {
            boolean requiresDownload = !existsGraphDirectory() && !existsOsmPbfFile();
            if (requiresDownload)
                log.fine("requiresDownload=" + requiresDownload +
                        " existsGraphDirectory()=" + existsGraphDirectory() + " getGraphDirectory()=" + getGraphDirectory() +
                        " existsOsmPbfFile()=" + existsOsmPbfFile() + " getOsmPbfFile()=" + getOsmPbfFile());
            return requiresDownload;
        }

        public void download() {
            fireDownloading();
            downloadAndWait(next);
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

        public boolean hasNextDownload() {
            return !downloadables.isEmpty();
        }

        public void nextDownload() {
            if (!hasNextDownload())
                return;

            this.next = downloadables.remove(0);
            setOsmPbfFile(createFile(next));
        }
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
        String url = getBaseUrl(downloadable.getDataSource()) + uri;
        Action action = Action.valueOf(downloadable.getDataSource().getAction());
        File file = action.equals(Extract) ? createDirectory(downloadable) : createFile(downloadable);
        return downloadManager.queueForDownload(getName() + " Routing Data: " + uri, url, action,
                new FileAndChecksum(file, downloadable.getLatestChecksum()), null);
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        Collection<Downloadable> downloadables = finder.getDownloadablesFor(mapDescriptors);
        long notExists = 0L;
        for (Downloadable downloadable : downloadables) {
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
        List<Downloadable> downloadables = finder.getDownloadablesFor(mapDescriptors);
        for (Downloadable downloadable : downloadables) {
            download(downloadable);
            // avoid multiple downloads when kurviger, mapsforge graphs and geofabrik PBFs are returned
            break;
        }
    }
}
