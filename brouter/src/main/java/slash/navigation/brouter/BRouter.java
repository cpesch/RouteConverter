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
import slash.navigation.common.*;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.*;
import slash.navigation.routing.*;
import slash.navigation.routing.RoutingResult.Validity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.getExtension;
import static slash.common.io.Files.removeExtension;
import static slash.navigation.common.Bearing.calculateBearing;
import static slash.navigation.download.Checksum.createChecksum;
import static slash.navigation.routing.RoutingResult.Validity.Invalid;
import static slash.navigation.routing.RoutingResult.Validity.Valid;

/**
 * Encapsulates access to the BRouter.
 *
 * @author Christian Pesch
 */

public class BRouter extends BaseRoutingService {
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
        if (getProfiles() != null) {
            File[] files = getProfilesDirectory().listFiles((dir, name) -> getExtension(name).equals(".brf"));
            if (files != null) {
                for (File file : files) {
                    result.add(new TravelMode(removeExtension(file.getName())));
                }
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

    private java.io.File getDirectory(DataSource dataSource) {
        String path = getPath() + separator + dataSource.getDirectory();
        java.io.File f = new java.io.File(path);
        if (!f.exists())
            path = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(path);
    }

    private java.io.File getProfilesDirectory() {
        return getDirectory(getProfiles());
    }

    private java.io.File createProfileFile(String key) {
        return new java.io.File(getProfilesDirectory(), key);
    }

    private java.io.File getSegmentsDirectory() {
        return getDirectory(getSegments());
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

    Set<String> createFileKeys(double longitude, double latitude) {
        // logic borrowed from RoutingEngine#preloadPosition in BRouter
        Set<String> result = new HashSet<>();
        for (double deltaLatitude = -0.1; deltaLatitude <= 0.1; deltaLatitude += 0.1) {
            for (double deltaLongitude = -0.1; deltaLongitude <= 0.1; deltaLongitude += 0.1) {
                result.add(createFileKey(longitude + deltaLongitude, latitude + deltaLatitude));
            }
        }
        return result;
    }

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode) {
        SecondCounter secondCounter = new SecondCounter() {
            protected void second(int second) {
                fireRouting(second);
            }
        };
        secondCounter.start();

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
                    return new RoutingResult(asList(from, to), new DistanceAndTime(calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), null), Invalid);
                }

                TravelMode firstTravelMode = availableTravelModes.get(0);
                profile = new File(getProfilesDirectory(), firstTravelMode.getName() + ".brf");
                log.warning(format("Failed to find profile for travel mode %s; using first travel mode %s", travelMode, firstTravelMode));
            }

            double bearing = Bearing.calculateBearing(from.getLongitude(), from.getLatitude(),
                    to.getLongitude(), to.getLatitude()).getDistance();
            long routingTimeout = (long) (3000L + bearing / 20.0);
            log.fine(format("Distance %f results to default routing timeout %d milliseconds", bearing, routingTimeout));

            RoutingContext routingContext = new RoutingContext();
            routingContext.localFunction = profile.getPath();

            RoutingEngine routingEngine = new RoutingEngine(null, null, getSegmentsDirectory().getPath(), createWaypoints(from, to), routingContext);
            routingEngine.quite = true;
            routingEngine.doRun(preferences.getLong("routingTimeout", routingTimeout));

            if (routingEngine.getErrorMessage() != null)
                log.severe(format("Error while routing between %s and %s: %s", from, to, routingEngine.getErrorMessage()));

            OsmTrack track = routingEngine.getFoundTrack();
            double distance = routingEngine.getDistance();
            Validity validity = routingEngine.getErrorMessage() == null ? Valid : Invalid;
            return new RoutingResult(asPositions(track), new DistanceAndTime(distance, getTime(track)), validity);
        } finally {
            secondCounter.stop();

            long end = currentTimeMillis();
            log.info("Routing from " + from + " to " + to + " took " + (end - start) + " milliseconds");
        }
    }

    private long getTime(OsmTrack track) {
        float s = track.nodes.size() < 2 ? 0 : track.nodes.get(track.nodes.size() - 1).getTime() - track.nodes.get(0).getTime();
        return (long)((s + 0.5) * 1000);
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


    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        Collection<Downloadable> downloadables = getDownloadablesFor(mapDescriptors.stream()
                .map(MapDescriptor::getBoundingBox)
                .collect(Collectors.toList())
        );
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
        return boundingBoxes.stream()
                .flatMap(boundingBox -> getDownloadablesFor(boundingBox).stream())
                .collect(toSet());
    }

    private Download downloadSegment(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getSegmentsBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " Routing Segment: " + uri, url, Action.valueOf(getSegments().getAction()),
                new FileAndChecksum(createSegmentFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }

    public void downloadRoutingData(List<MapDescriptor> mapDescriptors) {
        Collection<Downloadable> downloadables = getDownloadablesFor(mapDescriptors.stream()
                .map(MapDescriptor::getBoundingBox)
                .collect(toList())
        );
        for (Downloadable downloadable : downloadables) {
            downloadSegment(downloadable);
        }
    }

    private void downloadAndWait(Collection<Downloadable> segments) {
        Collection<Download> downloads = new HashSet<>();
        for (Downloadable downloadable : segments)
            downloads.add(downloadSegment(downloadable));

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    public DownloadFuture downloadRoutingDataFor(String mapIdentifier, List<LongitudeAndLatitude> longitudeAndLatitudes) {
        if (!isInitialized()) {
            return new DownloadFutureImpl(Collections.emptySet());
        }
        Collection<String> uris = new HashSet<>();

        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            uris.addAll(createFileKeys(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Collection<Downloadable> segments = new HashSet<>();
        Checksum latestChecksum = null;

        for (String key : uris) {
            Downloadable downloadable = getSegments().getDownloadable(key);
            if (downloadable != null) {
                File file = createSegmentFile(downloadable.getUri());
                if (!file.exists())
                    segments.add(downloadable);

                if (latestChecksum == null || downloadable.getLatestChecksum() != null &&
                        downloadable.getLatestChecksum().laterThan(latestChecksum))
                    latestChecksum = downloadable.getLatestChecksum();
            }
        }

        // all segments have to be from the same (latest) date
        if (latestChecksum != null) {
            for (String key : uris) {
                Downloadable downloadable = getSegments().getDownloadable(key);
                if (downloadable != null) {
                    File file = createSegmentFile(downloadable.getUri());
                    if (file.exists()) {
                        Checksum fileChecksum = null;
                        try {
                            fileChecksum = createChecksum(file, false);
                        } catch (IOException e) {
                            log.warning(format("Cannot calculate checksum for %s: %s", file, e.getLocalizedMessage()));
                        }

                        if (fileChecksum != null && latestChecksum.laterThan(fileChecksum))
                            segments.add(downloadable);
                    }
                }
            }
        }
        return new DownloadFutureImpl(segments);
    }

    private class DownloadFutureImpl implements DownloadFuture {
        private final Collection<Downloadable> segments;

        public DownloadFutureImpl(Collection<Downloadable> segments) {
            this.segments = segments;
        }

        public boolean isRequiresDownload() {
            return !segments.isEmpty();
        }

        public void download() {
            fireDownloading();
            downloadAndWait(segments);
            segments.clear();
        }

        public boolean isRequiresProcessing() {
            return false;
        }

        public void process() {
        }
    }


    private void downloadProfile(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getProfilesBaseUrl() + uri;
        downloadManager.queueForDownload(getName() + " Routing Profile: " + uri, url, Action.valueOf(getProfiles().getAction()),
                new FileAndChecksum(createProfileFile(downloadable.getUri()), downloadable.getLatestChecksum()), null);
    }

    public void downloadProfiles() {
        if (isInitialized()) {
            for (Downloadable downloadable : getProfiles().getFiles()) {
                if (!createProfileFile(downloadable.getUri()).exists())
                    downloadProfile(downloadable);
            }
        }
    }
}
