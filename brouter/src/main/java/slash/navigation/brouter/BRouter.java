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

import btools.expressions.BExpressionMetaData;
import btools.mapaccess.PhysicalFile;
import btools.router.*;
import slash.navigation.common.*;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.*;
import slash.navigation.routing.*;
import slash.navigation.routing.RoutingResult.Validity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.removeExtension;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.common.Bearing.calculateBearing;
import static slash.navigation.download.Checksum.createChecksum;
import static slash.navigation.routing.RoutingResult.Validity.Invalid;
import static slash.navigation.routing.RoutingResult.Validity.Valid;
import static slash.navigation.routing.TravelRestrictions.NO_RESTRICTIONS;

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
    public static final String DOT_BRF = ".brf";
    public static final String DOT_RD5 = ".rd5";
    private static final String LOOKUPS_DAT = "lookups.dat";

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

        File segmentsDirectory = getSegmentsDirectory();
        if (segmentsDirectory != null) {
            File storageConfig = new File(segmentsDirectory, "storageconfig.txt");
            try {
                PrintWriter writer = new PrintWriter(storageConfig);
                writer.println("secondary_segment_dir=" + segmentsDirectory.getPath());
                writer.close();
            } catch (FileNotFoundException e) {
                log.severe(format("Error while writing storage config: %s", getLocalizedMessage(e)));
            }
        }

        refreshLookupsIfStale();
        removeOutdatedSegments();
    }

    /**
     * Blocks until a locally cached {@code lookups.dat} that no longer matches the latest expected
     * checksum has been re-downloaded. Must run before {@link #removeOutdatedSegments()}, which reads
     * the local file's embedded lookup version synchronously right afterwards: a stale {@code
     * lookups.dat} would otherwise make {@code removeOutdatedSegments()} misjudge already-current
     * segments as outdated and delete them, and the fixed-up version would only take effect after a
     * restart. Does nothing if the file is missing (nothing to compare against yet -- handled by the
     * regular, asynchronous {@link #downloadProfiles()} instead), the datasource has no matching entry,
     * or no download manager is available (e.g. in hermetic tests).
     */
    private void refreshLookupsIfStale() {
        if (downloadManager == null)
            return;

        File profilesDirectory = getProfilesDirectory();
        if (profilesDirectory == null)
            return;

        File lookups = new File(profilesDirectory, LOOKUPS_DAT);
        if (!lookups.exists())
            return;

        Downloadable downloadable = getProfiles().getDownloadable(LOOKUPS_DAT);
        if (downloadable == null)
            return;

        try {
            Checksum actual = createChecksum(lookups, true);
            if (actual == null || profileFileNeedsRefresh(actual.getSHA1(), actual.getContentLength(), downloadable.getChecksums())) {
                Download download = downloadProfile(downloadable);
                downloadManager.waitForCompletion(Collections.singletonList(download));
            }
        } catch (IOException e) {
            log.warning(format("Cannot calculate checksum for profile %s: %s", lookups, getLocalizedMessage(e)));
        }
    }

    /**
     * Reads the lookup version bundled with the BRouter library (from {@code lookups.dat}) and
     * removes any locally cached {@code .rd5} segment whose embedded lookup version is <em>older</em>.
     * Such segments predate a BRouter format bump and would otherwise make routing fail hard with
     * "lookup version mismatch (old rd5?)". Removed segments are re-downloaded on demand by the
     * regular routing path, so this method only deletes and never starts a download itself.
     * <p>
     * Segments that are <em>newer</em> than the local {@code lookups.dat} are deliberately kept: that
     * skew means {@code lookups.dat} itself is stale (the profiles datasource has not caught up with a
     * BRouter format bump yet, so {@link #refreshLookupsIfStale()} could not update it). Deleting them
     * would only trigger a re-download of the same newer version on the next routing request and again
     * on the next launch -- an endless re-download loop of large segment files. We log an actionable
     * hint instead and leave the segment in place.
     */
    void removeOutdatedSegments() {
        File profilesDirectory = getProfilesDirectory();
        if (profilesDirectory == null)
            return;

        File segmentsDirectory = getSegmentsDirectory();
        if (segmentsDirectory == null)
            return;

        File lookups = new File(profilesDirectory, LOOKUPS_DAT);
        if (!lookups.exists())
            return;

        int expectedVersion;
        try {
            BExpressionMetaData meta = new BExpressionMetaData();
            meta.readMetaData(lookups);
            expectedVersion = meta.lookupVersion;
        } catch (Exception e) {
            log.warning(format("Cannot read lookup version from %s: %s", lookups, getLocalizedMessage(e)));
            return;
        }

        for (File file : collectFiles(segmentsDirectory, DOT_RD5)) {
            int version;
            try {
                version = PhysicalFile.checkVersionIntegrity(file);
            } catch (Exception e) {
                log.warning(format("Cannot read lookup version from segment %s: %s", file, getLocalizedMessage(e)));
                continue;
            }
            if (version == expectedVersion)
                continue;

            if (version > expectedVersion) {
                log.warning(format("Keeping BRouter segment %s with newer lookup version %d than %s (%d): " +
                                "the profiles datasource is stale, deleting would only re-download the same version",
                        file, version, LOOKUPS_DAT, expectedVersion));
                continue;
            }

            log.warning(format("Removing outdated BRouter segment %s with lookup version %d (expected %d)", file, version, expectedVersion));
            if (!file.delete())
                log.warning(format("Cannot delete outdated BRouter segment %s", file));
        }
    }

    public boolean isDownload() {
        return true;
    }

    public List<TravelMode> getAvailableTravelModes() {
        List<TravelMode> result = new ArrayList<>();
        if (getProfiles() != null) {
            List<File> files = collectFiles(getProfilesDirectory(), DOT_BRF);
            for (File file : files) {
                result.add(new TravelMode(removeExtension(file.getName())));
            }
        }
        return result;
    }

    public TravelMode getPreferredTravelMode() {
        return MOPED;
    }

    public TravelRestrictions getAvailableTravelRestrictions() {
        return NO_RESTRICTIONS;
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
        if(dataSource == null)
            return null;

        String path = getPath() + separator + dataSource.getDirectory();
        if (isEmpty(path) || !new java.io.File(path).exists())
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

        return format("%s%d_%s%d" + DOT_RD5,
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

    public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode, TravelRestrictions travelRestrictions) {
        SecondCounter secondCounter = new SecondCounter() {
            protected void second(int second) {
                fireRouting(second);
            }
        };
        secondCounter.start();

        long start = currentTimeMillis();
        try {
            File profilesDirectory = getProfilesDirectory();
            if(profilesDirectory == null) {
                log.warning(format("Cannot route between %s and %s: no profiles directory found", from, to));
                return new RoutingResult(asList(from, to), new DistanceAndTime(calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), null), Invalid);
            }
            File segmentsDirectory = getSegmentsDirectory();
            if(segmentsDirectory == null) {
                log.warning(format("Cannot route between %s and %s: no segments directory found", from, to));
                return new RoutingResult(asList(from, to), new DistanceAndTime(calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), null), Invalid);
            }
            File profile = new File(profilesDirectory, travelMode.name() + ".brf");
            if (!profile.exists()) {
                profile = new File(profilesDirectory, getPreferredTravelMode().name() + ".brf");
                log.warning(format("Failed to find profile for travel mode %s; using preferred travel mode %s", travelMode, getPreferredTravelMode()));
            }
            if (!profile.exists()) {
                List<TravelMode> availableTravelModes = getAvailableTravelModes();
                if (availableTravelModes.isEmpty()) {
                    log.warning(format("Cannot route between %s and %s: no travel modes found in %s", from, to, profilesDirectory));
                    return new RoutingResult(asList(from, to), new DistanceAndTime(calculateBearing(from.getLongitude(), from.getLatitude(), to.getLongitude(), to.getLatitude()).getDistance(), null), Invalid);
                }

                TravelMode firstTravelMode = availableTravelModes.get(0);
                profile = new File(profilesDirectory, firstTravelMode.name() + ".brf");
                log.warning(format("Failed to find profile for travel mode %s; using first travel mode %s", travelMode, firstTravelMode));
            }

            double bearing = Bearing.calculateBearing(from.getLongitude(), from.getLatitude(),
                    to.getLongitude(), to.getLatitude()).getDistance();
            long routingTimeout = (long) (10000L + bearing / 15.0);
            log.fine(format("Distance %f results to default routing timeout %d milliseconds", bearing, routingTimeout));

            RoutingContext routingContext = new RoutingContext();
            routingContext.localFunction = profile.getPath();

            RoutingEngine routingEngine = new RoutingEngine(null, null, segmentsDirectory, createWaypoints(from, to), routingContext);
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

    private static final double DUPLICATE_OFFSET = 0.0001;

    public NavigationPosition getSnapToRoadPosition(NavigationPosition position) {
        NavigationPosition duplicate = new SimpleNavigationPosition(position.getLongitude() + DUPLICATE_OFFSET, position.getLatitude() + DUPLICATE_OFFSET);
        RoutingResult result = getRouteBetween(position, duplicate, getPreferredTravelMode(), NO_RESTRICTIONS);
        NavigationPosition snapPosition = result.validity().equals(Valid) && !result.positions().isEmpty() ? result.positions().get(0) : null;
        if (snapPosition != null) {
            double bearing = Bearing.calculateBearing(position.getLongitude(), position.getLatitude(),
                    snapPosition.getLongitude(), snapPosition.getLatitude()).getDistance();
            log.info(format("Found snapping position %s for %s with distance %s", snapPosition, position, bearing));
            if (bearing < 100.0)
                return snapPosition;
        }
        return null;
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

        double longitude = boundingBox.southWest().getLongitude();
        while (longitude < boundingBox.northEast().getLongitude()) {

            double latitude = boundingBox.southWest().getLatitude();
            while (latitude < boundingBox.northEast().getLatitude()) {
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
                FileAndChecksum.forChecksums(createSegmentFile(downloadable.getUri()), downloadable.getChecksums()), null);
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

    private Collection<Downloadable> collectDownloadables(Collection<String> uris) {
        Collection<Downloadable> result = new HashSet<>();

        for (String key : uris) {
            Downloadable downloadable = getSegments().getDownloadable(key);
            if (downloadable == null) {
                log.warning(format("Cannot find downloadable for segment %s", key));
                continue;
            }
            result.add(downloadable);
        }
        return result;
    }

    private boolean existAllSegmentsFromSameDay(Collection<Downloadable> segments) {
        Checksum latestChecksum = null;

        for (Downloadable downloadable : segments) {
            File file = createSegmentFile(downloadable.getUri());
            Checksum fileChecksum = null;
            try {
                fileChecksum = createChecksum(file, false);
            } catch (IOException e) {
                log.warning(format("Cannot calculate checksum for %s: %s", file, e.getLocalizedMessage()));
            }

            // file does not exist or failed to calculate checksum
            if (fileChecksum == null)
                return false;

            if (latestChecksum == null)
                latestChecksum = fileChecksum;

            // file is from a different day than existing file
            else if (!fileChecksum.sameDay(latestChecksum))
                return false;
        }
        return true;
    }

    public DownloadFuture downloadRoutingDataFor(String mapIdentifier, List<LongitudeAndLatitude> longitudeAndLatitudes) {
        if (!isInitialized()) {
            return new DownloadFutureImpl(Collections.emptySet());
        }
        Collection<String> uris = new HashSet<>();

        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            uris.addAll(createFileKeys(longitudeAndLatitude.longitude(), longitudeAndLatitude.latitude()));
        }

        Collection<Downloadable> segments = collectDownloadables(uris);
        // if all segments exist locally and are from the same day, we don't need to download them
        if(existAllSegmentsFromSameDay(segments))
            segments = new HashSet<>();
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


    private Download downloadProfile(Downloadable downloadable) {
        String uri = downloadable.getUri();
        String url = getProfilesBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " Routing Profile: " + uri, url, Action.valueOf(getProfiles().getAction()),
                FileAndChecksum.forChecksums(createProfileFile(downloadable.getUri()), downloadable.getChecksums()), null);
    }

    /**
     * Returns {@code true} if a local profile file with the given actual SHA-1 and content length
     * must be re-downloaded because it does not match the latest expected checksum (by
     * {@link Checksum#getLastModified()}) among {@code expectedChecksums}. Returns {@code false}
     * when {@code expectedChecksums} is null/empty (undecidable -- keep the existing file).
     */
    static boolean profileFileNeedsRefresh(String localSha1, Long localContentLength, List<Checksum> expectedChecksums) {
        if (expectedChecksums == null || expectedChecksums.isEmpty())
            return false;

        Checksum latest = Checksum.getLatestChecksum(expectedChecksums);
        if (latest == null)
            return false;

        return !Objects.equals(localSha1, latest.getSHA1()) || !Objects.equals(localContentLength, latest.getContentLength());
    }

    public void downloadProfiles() {
        if (isInitialized()) {
            for (Downloadable downloadable : getProfiles().getFiles()) {
                File file = createProfileFile(downloadable.getUri());
                if (!file.exists()) {
                    downloadProfile(downloadable);
                    continue;
                }

                try {
                    Checksum actual = createChecksum(file, true);
                    if (actual != null && profileFileNeedsRefresh(actual.getSHA1(), actual.getContentLength(), downloadable.getChecksums()))
                        downloadProfile(downloadable);
                } catch (IOException e) {
                    log.warning(format("Cannot calculate checksum for profile %s: %s", file, getLocalizedMessage(e)));
                }
            }
        }
    }
}
