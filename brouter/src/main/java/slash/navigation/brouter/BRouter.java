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

import btools.router.OsmNodeNamed;
import btools.router.OsmPathElement;
import btools.router.OsmTrack;
import btools.router.RoutingContext;
import btools.router.RoutingEngine;
import slash.navigation.common.Bearing;
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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

    private final DownloadManager downloadManager;
    private DataSource profiles, segments;

    public BRouter(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return "BRouter";
    }

    public synchronized boolean isInitialized() {
        return getProfiles() != null && getSegments() != null;
    }

    private synchronized DataSource getProfiles() {
        return profiles;
    }

    private synchronized DataSource getSegments() {
        return segments;
    }

    public synchronized void setProfilesAndSegments(DataSource profiles, DataSource segments) {
        this.profiles = profiles;
        this.segments = segments;
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
        List<TravelMode> result = new ArrayList<>();
        File[] files = getProfilesDirectory().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return getExtension(name).equals(".brf");
            }
        });
        if (files != null) {
            for (File file : files) {
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
        return preferences.get(PROFILES_BASE_URL_PREFERENCE, getProfiles().getBaseUrl());
    }

    private String getSegmentsBaseUrl() {
        return preferences.get(SEGMENTS_BASE_URL_PREFERENCE, getSegments().getBaseUrl());
    }

    private java.io.File getDirectory(DataSource dataSource, String directoryName) {
        String path = getPath() + "/" + directoryName;
        java.io.File f = new java.io.File(path);
        if (!f.exists())
            directoryName = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    private java.io.File getProfilesDirectory() {
        return getDirectory(getProfiles(), "profiles");
    }

    private java.io.File createProfileFile(String key) {
        return new java.io.File(getProfilesDirectory(), key);
    }

    private java.io.File getSegmentsDirectory() {
        return getDirectory(getSegments(), "segments4");
    }

    private java.io.File createSegmentFile(String key) {
        return new java.io.File(getSegmentsDirectory(), key);
    }

    String createFileKey(double longitude, double latitude) {
        // code borrowed from NodesCache in BRouter
        int lonDegree = asLongitude(longitude) / 1000000;
        int latDegree = asLatitude(latitude) / 1000000;

        int lonMod5 = lonDegree % 5;
        int latMod5 = latDegree % 5;

        int lon = lonDegree - 180 - lonMod5;
        int lat = latDegree - 90 - latMod5;

        return format("%s%d_%s%d.rd5",
                lon < 0 ? "W" : "E",
                lon < 0 ? -lon : lon,
                lat < 0 ? "S" : "N",
                lat < 0 ? -lat : lat);
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
                    return new RoutingResult(asList(from, to), new DistanceAndTime(calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), null), false);
                }

                TravelMode firstTravelMode = availableTravelModes.get(0);
                profile = new File(getProfilesDirectory(), firstTravelMode.getName() + ".brf");
                log.warning(format("Failed to find profile for travel mode %s; using first travel mode %s", travelMode, firstTravelMode));
            }

            double bearing = Bearing.calculateBearing(from.getLongitude(), from.getLatitude(),
                    to.getLongitude(), to.getLatitude()).getDistance();
            long routingTimeout = (long) (1000L + bearing / 20.0);
            log.info(format("Distance %f results to default routing timeout %d milliseconds", bearing, routingTimeout));

            RoutingContext routingContext = new RoutingContext();
            routingContext.localFunction = profile.getPath();

            RoutingEngine routingEngine = new RoutingEngine(null, null, getSegmentsDirectory().getPath(), createWaypoints(from, to), routingContext);
            routingEngine.quite = true;
            routingEngine.doRun(preferences.getLong("routingTimeout", routingTimeout));

            if (routingEngine.getErrorMessage() != null)
                log.severe(format("Error while routing between %s and %s: %s", from, to, routingEngine.getErrorMessage()));

            OsmTrack track = routingEngine.getFoundTrack();
            double distance = routingEngine.getDistance();
            return new RoutingResult(asPositions(track), new DistanceAndTime(distance, null), routingEngine.getErrorMessage() == null);
        } finally {
            long end = currentTimeMillis();
            log.info("BRouter: routing from " + from + " to " + to + " took " + (end - start) + " milliseconds");
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
            result.add(new SimpleNavigationPosition(asLongitude(element.getILon()), asLatitude(element.getILat()), element.getElev(), null));
        }
        return result;
    }

    double asLongitude(int longitude) {
        return (longitude / 1000000.0) - 180.0;
    }

    double asLatitude(int latitude) {
        return (latitude / 1000000.0) - 90.0;
    }

    public DownloadFuture downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Collection<String> uris = new HashSet<>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            uris.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        final Collection<Downloadable> notExistingSegments = new HashSet<>();
        if (isInitialized()) {
            for (String key : uris) {
                Downloadable downloadable = getSegments().getDownloadable(key);
                if (downloadable != null) {
                    if (!createSegmentFile(downloadable.getUri()).exists())
                        notExistingSegments.add(downloadable);
                }
            }
        }

        final Collection<Downloadable> notExistingProfiles = new HashSet<>();
        if (isInitialized()) {
            for (Downloadable downloadable : getProfiles().getFiles()) {
                if (!createProfileFile(downloadable.getUri()).exists())
                    notExistingProfiles.add(downloadable);
            }
        }

        return new DownloadFuture() {
            public boolean isRequiresDownload() {
                return !notExistingProfiles.isEmpty() || !notExistingSegments.isEmpty();
            }

            public boolean isRequiresProcessing() {
                return false;
            }

            public void download() {
                downloadAndWait(notExistingProfiles, notExistingSegments);
            }

            public void process() {
            }
        };
    }

    private void downloadAndWait(Collection<Downloadable> profiles, Collection<Downloadable> segments) {
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
        return downloadManager.queueForDownload(getName() + " Routing Profile: " + uri, url, Action.valueOf(getProfiles().getAction()),
                new FileAndChecksum(createProfileFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }

    private Download downloadSegment(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getSegmentsBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " Routing Segment: " + uri, url, Action.valueOf(getSegments().getAction()),
                new FileAndChecksum(createSegmentFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }

    private Collection<Downloadable> getDownloadablesFor(BoundingBox boundingBox) {
        Collection<Downloadable> result = new HashSet<>();

        double longitude = boundingBox.getSouthWest().getLongitude();
        while (longitude < boundingBox.getNorthEast().getLongitude()) {

            double latitude = boundingBox.getSouthWest().getLatitude();
            while (latitude < boundingBox.getNorthEast().getLatitude()) {
                String key = createFileKey(longitude, latitude);
                Downloadable downloadable = getSegments().getDownloadable(key);
                if (downloadable != null)
                    result.add(downloadable);
                latitude += 1.0;
            }

            longitude += 1.0;
        }
        return result;
    }

    private Collection<Downloadable> getDownloadablesFor(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> result = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes)
            result.addAll(getDownloadablesFor(boundingBox));
        return result;
    }

    public long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> downloadables = getDownloadablesFor(boundingBoxes);
        long notExists = 0L;
        for (Downloadable downloadable : downloadables) {
            Long contentLength = downloadable.getLatestChecksum().getContentLength();
            if (contentLength == null)
                continue;

            java.io.File file = createSegmentFile(downloadable.getUri());
            if (!file.exists())
                notExists += contentLength;
        }
        return notExists;
    }

    public void downloadRoutingData(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> downloadables = getDownloadablesFor(boundingBoxes);
        for (Downloadable downloadable : downloadables) {
            downloadSegment(downloadable);
        }
    }
}
