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
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.getExtension;
import static slash.common.io.Files.removeExtension;
import static slash.navigation.common.Bearing.calculateBearing;
import static slash.navigation.download.Action.Copy;

/**
 * Encapsulates access to the BRouter.
 *
 * @author Christian Pesch
 */

public class BRouter implements RoutingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(BRouter.class);
    private static final Logger log = Logger.getLogger(BRouter.class.getName());
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String PROFILES_BASE_URL_PREFERENCE = "profilesBaseUrl";
    private static final String SEGMENTS_BASE_URL_PREFERENCE = "segmentsBaseUrl";
    private static final TravelMode MOPED = new TravelMode("moped");

    private DataSource profiles, segments;
    private DownloadManager downloadManager;

    private final RoutingContext routingContext = new RoutingContext();

    public synchronized void setDataSource(DataSource profiles, DataSource segments, DownloadManager downloadManager) {
        this.profiles = profiles;
        this.segments = segments;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return "BRouter";
    }

    public synchronized boolean isInitialized() {
        return profiles != null && segments != null && downloadManager != null;
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isSupportTurnpoints() {
        return false;
    }

    public List<TravelMode> getAvailableTravelModes() {
        List<TravelMode> result = new ArrayList<>();
        File[] files = getProfilesDirectory().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return getExtension(name).equals(".brf");
            }
        });
        if (files != null) {
            for(File file : files) {
                result.add(new TravelMode(removeExtension(file.getName())));
            }
        }
        return result;
    }

    public TravelMode getPreferredTravelMode() {
        return MOPED;
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    private String getProfilesBaseUrl() {
        return preferences.get(PROFILES_BASE_URL_PREFERENCE, profiles.getBaseUrl());
    }

    private String getSegmentsBaseUrl() {
        return preferences.get(SEGMENTS_BASE_URL_PREFERENCE, segments.getBaseUrl());
    }

    private java.io.File getDirectory(DataSource dataSource, String directoryName) {
        String path = getPath() + "/" + directoryName;
        java.io.File f = new java.io.File(path);
        if (!f.exists())
            directoryName = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    private java.io.File getProfilesDirectory() {
        return getDirectory(profiles, "profiles");
    }

    private java.io.File createProfileFile(String key) {
        return new java.io.File(getProfilesDirectory(), key);
    }

    private java.io.File getSegmentsDirectory() {
        return getDirectory(segments, "segments");
    }

    private java.io.File createSegmentFile(String key) {
        return new java.io.File(getSegmentsDirectory(), key);
    }

    String createFileKey(double longitude, double latitude) {
        int longitudeAsInteger = ((int) longitude / 5) * 5;
        int latitudeAsInteger = ((int) latitude / 5) * 5;
        return format("%s%d_%s%d.rd5",
                longitude < 0 ? "W" : "E",
                longitude < 0 ? -longitudeAsInteger : longitudeAsInteger,
                latitude < 0 ? "S" : "N",
                latitude < 0 ? -latitudeAsInteger : latitudeAsInteger);
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        long start = currentTimeMillis();
        try {
            File profile = new File(getProfilesDirectory(), travelMode.getName() + ".brf");
            if (!profile.exists()) {
                profile = new File(getProfilesDirectory(), getPreferredTravelMode().getName() + ".brf");
                log.warning(format("Failed to find profile for travel mode %s; using preferred travel mode %s", travelMode, getPreferredTravelMode()));
            }
            if (!profile.exists()) {
                List<TravelMode> availableTravelModes = getAvailableTravelModes();
                if (availableTravelModes.size() == 0) {
                    log.warning(format("Cannot route between %s and %s: no travel modes found in %s", from, to, getProfilesDirectory()));
                    return new RoutingResult(asList(from, to), calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), 0L, false);
                }

                TravelMode firstTravelMode = availableTravelModes.get(0);
                profile = new File(getProfilesDirectory(), firstTravelMode.getName() + ".brf");
                log.warning(format("Failed to find profile for travel mode %s; using first travel mode %s", travelMode, firstTravelMode));
            }
            routingContext.localFunction = profile.getPath();

            RoutingEngine routingEngine = new RoutingEngine(null, null, getSegmentsDirectory().getPath(), createWaypoints(from, to), routingContext);
            routingEngine.quite = true;
            routingEngine.doRun(preferences.getLong("routingTimeout", 30 * 1000L));
            if (routingEngine.getErrorMessage() != null) {
                log.warning(format("Cannot route between %s and %s: %s", from, to, routingEngine.getErrorMessage()));
                return new RoutingResult(asList(from, to), calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), 0L, false);
            }

            OsmTrack track = routingEngine.getFoundTrack();
            int distance = routingEngine.getDistance();
            return new RoutingResult(asPositions(track), distance, 0L, true);
        } finally {
            long end = currentTimeMillis();
            System.out.println(getClass().getSimpleName() + ": routing took " + (end - start) + " milliseconds");
        }
    }

    private List<OsmNodeNamed> createWaypoints(NavigationPosition from, NavigationPosition to) {
        List<OsmNodeNamed> result = new ArrayList<>();
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
        List<NavigationPosition> result = new ArrayList<>();
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
        Set<String> uris = new HashSet<>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            uris.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Collection<Downloadable> downloadableSegments = new HashSet<>();
        for (String key : uris) {
            Downloadable downloadable = segments.getDownloadable(key);
            if (downloadable != null)
                downloadableSegments.add(downloadable);
        }

        final Collection<Downloadable> notExistingProfiles = new HashSet<>();
        for (Downloadable downloadable : profiles.getFiles()) {
            if (createProfileFile(downloadable.getUri()).exists())
                continue;
            notExistingProfiles.add(downloadable);
        }

        final Collection<Downloadable> notExistingSegments = new HashSet<>();
        for (Downloadable downloadable : downloadableSegments) {
            if (createSegmentFile(downloadable.getUri()).exists())
                continue;
            notExistingSegments.add(downloadable);
        }

        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return !notExistingProfiles.isEmpty() || !notExistingSegments.isEmpty();
            }

            public boolean isRequiresProcessing() {
                return false;
            }

            public void download() {
                downloadAll(notExistingProfiles, notExistingSegments);
            }

            public void process() {
                // intentionally do nothing
            }
        };
    }

    private void downloadAll(Collection<Downloadable> profiles, Collection<Downloadable> segments) {
        Collection<Download> downloads = new HashSet<>();
        for (Downloadable downloadable : profiles)
            downloads.add(downloadProfile(downloadable));
        for (Downloadable downloadable : segments)
            downloads.add(downloadSegment(downloadable));

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private Download downloadProfile(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getProfilesBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + ": Routing Profile " + uri, url, Copy,
                null, new FileAndChecksum(createProfileFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }

    private Download downloadSegment(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getSegmentsBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + ": Routing Segment " + uri, url, Copy,
                null, new FileAndChecksum(createSegmentFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }
}
