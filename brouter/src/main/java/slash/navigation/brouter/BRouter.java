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
package slash.navigation.brouter;

import btools.router.*;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Externalization.extractFile;

/**
 * Encapsulates access to the BRouter.
 *
 * @author Christian Pesch
 */

public class BRouter implements RoutingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(BRouter.class);
    private static final Logger log = Logger.getLogger(BRouter.class.getName());
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";
    private static final String DATASOURCE_URL = "brouter-datasources.xml";

    private static final int MAX_RUNNING_TIME = 1000;
    private static final List<TravelMode> TRAVEL_MODES = asList(new TravelMode("fastbike"),
            new TravelMode("moped"), new TravelMode("safety"), new TravelMode("shortest"), new TravelMode("trekking"),
            new TravelMode("trekking-ignore-cr"), new TravelMode("trekking-noferries"),
            new TravelMode("trekking-nosteps"), new TravelMode("trekking-steep"), new TravelMode("car-test"));

    private final DataSource dataSource;
    private final DownloadManager downloadManager;

    private final RoutingContext routingContext = new RoutingContext();
    private boolean initialized = false;

    public BRouter(DataSource dataSource, DownloadManager downloadManager) {
        this.dataSource = dataSource;
        this.downloadManager = downloadManager;
    }

    private void initialize() {
        try {
            extractFile("slash/navigation/brouter/lookups.dat");
        } catch (IOException e) {
            log.warning("Cannot initialize BRouter: " + e);
        }
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
        initialize();
        try {
            routingContext.localFunction = extractFile("slash/navigation/brouter/" + travelMode.getName() + ".brf").getPath();
        } catch (IOException e) {
            log.warning(format("Cannot configure travel mode %s: %s", travelMode, e));
        }

        RoutingEngine routingEngine = new RoutingEngine(null, null, getDirectory().getPath(), createWaypoints(from, to), routingContext);
        routingEngine.quite = true;
        routingEngine.doRun(MAX_RUNNING_TIME);
        if (routingEngine.getErrorMessage() != null) {
            log.warning(format("Cannot route between %s and %s: %s", from, to, routingEngine.getErrorMessage()));
            return null;
        }

        OsmTrack track = routingEngine.getFoundTrack();
        int distance = routingEngine.getDistance();
        return new RoutingResult(asPositions(track), distance, 0);
    }

    private List<OsmNodeNamed> createWaypoints(NavigationPosition from, NavigationPosition to) {
        List<OsmNodeNamed> result = new ArrayList<OsmNodeNamed>();
        result.add(asOsmNodeNamed(from.getDescription(), from.getLongitude(), from.getLatitude()));
        result.add(asOsmNodeNamed(to.getDescription(), to.getLongitude(), to.getLatitude()));
        return result;
    }

    private OsmNodeNamed asOsmNodeNamed(String name, Double toLongitude, Double toLatitude) {
        OsmNodeNamed result = new OsmNodeNamed();
        result.name = name;
        result.ilon = asLongitude(toLongitude);
        result.ilat = asLatitude(toLatitude);
        return result;
    }

    int asLongitude(Double longitude) {
        return longitude != null ? (int) ((longitude + 180.0) * 1000000.0 + 0.5) : 0;
    }

    int asLatitude(Double latitude) {
        return latitude != null ? (int) ((latitude + 90.0) * 1000000.0 + 0.5) : 0;
    }

    private List<NavigationPosition> asPositions(OsmTrack track) {
        List<NavigationPosition> result = new ArrayList<NavigationPosition>();
        for (OsmPathElement element : track.nodes) {
            result.add(asPosition(element));
        }
        return result;
    }

    private NavigationPosition asPosition(OsmPathElement element) {
        return new SimpleNavigationPosition(asLongitude(element.getILon()), asLatitude(element.getILat()), element.getElev(), element.message);
    }

    double asLongitude(int longitude) {
        return (longitude / 1000000.0) - 180.0;
    }

    double asLatitude(int latitude) {
        return (latitude / 1000000.0) - 90.0;
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        initialize();

        Set<String> keys = createKeys(longitudeAndLatitudes);
        /* TODO
        Set<FileAndTarget> files = createFileAndTargets(keys);
        final Set<FileAndTarget> notExistingFiles = createNotExistingFiles(files);

        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return !notExistingFiles.isEmpty();
            }

            public boolean isRequiresProcessing() {
                return false;
            }

            public void download() {
                downloadFiles(notExistingFiles);
            }

            public void process() {
                // intentionally do nothing
            }
        };
        */
        return null;
    }

    private Set<String> createKeys(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<String> keys = new HashSet<String>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }
        return keys;
    }

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), format("%s%s", key, ".rd5"));
    }

    String createFileKey(double longitude, double latitude) {
        int longitudeAsInteger = ((int) longitude / 5) * 5;
        int latitudeAsInteger = ((int) latitude / 5) * 5;
        return format("%s%d_%s%d",
                longitude < 0 ? "W" : "E",
                longitude < 0 ? -longitudeAsInteger : longitudeAsInteger,
                latitude < 0 ? "S" : "N",
                latitude < 0 ? -latitudeAsInteger : latitudeAsInteger);
    }

    /* TODO
    private Set<FileAndTarget> createFileAndTargets(Set<String> keys) {
        Set<FileAndTarget> files = new HashSet<FileAndTarget>();
        for (String key : keys) {
            File catalog = fileMap.get(key + ".rd5");
            if (catalog != null)
                files.add(new FileAndTarget(catalog, createFile(key)));
        }
        return files;
    }

    private Set<FileAndTarget> createNotExistingFiles(Set<FileAndTarget> files) {
        final Set<FileAndTarget> notExistingFiles = new HashSet<FileAndTarget>();
        for (FileAndTarget file : files) {
            if (new Validator(file.target).existsFile())
                continue;
            notExistingFiles.add(file);
        }
        return notExistingFiles;
    }

    private void downloadFiles(Set<FileAndTarget> retrieve) {
        Collection<Download> downloads = new HashSet<Download>();
        for (FileAndTarget file : retrieve)
            downloads.add(initiateDownload(file));

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private Download initiateDownload(FileAndTarget file) {
        String uri = file.file.getUri();
        String url = getBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " routing data for " + uri, url,
                file.file.getSize(), file.file.getChecksum(), file.file.getTimestamp(), Copy, file.target);
    }
    */
}
