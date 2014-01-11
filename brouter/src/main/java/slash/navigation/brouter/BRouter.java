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
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.RoutingService;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
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
    private static final int MAX_RUNNING_TIME = 1000;

    private final DownloadManager downloadManager;
    private final RoutingContext routingContext = new RoutingContext();

    public BRouter(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void initialize() throws IOException {
        extractFile("slash/navigation/brouter/car-test.brf");
        extractFile("slash/navigation/brouter/fastbike.brf");
        extractFile("slash/navigation/brouter/lookups.dat");
        extractFile("slash/navigation/brouter/moped.brf");
        extractFile("slash/navigation/brouter/safety.brf");
        extractFile("slash/navigation/brouter/shortest.brf");
        File profileFile = extractFile("slash/navigation/brouter/trekking.brf");
        extractFile("slash/navigation/brouter/trekking-ignore-cr.brf");
        extractFile("slash/navigation/brouter/trekking-noferries.brf");
        extractFile("slash/navigation/brouter/trekking-nosteps.brf");
        extractFile("slash/navigation/brouter/trekking-steep.brf");

        routingContext.localFunction = profileFile.getPath();
    }

    public String getName() {
        return "BRouter";
    }

    private String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE, "http://h2096617.stratoserver.net/brouter/");
    }

    public List<NavigationPosition> getRouteBetween(NavigationPosition from, NavigationPosition to) {
        RoutingEngine routingEngine = new RoutingEngine(null, null, getDirectory().getPath(), createWaypoints(from, to), routingContext);
        routingEngine.quite = true;
        routingEngine.doRun(MAX_RUNNING_TIME);
        if (routingEngine.getErrorMessage() != null) {
            log.warning(format("Cannot route between %s and %s: %s", from, to, routingEngine.getErrorMessage()));
            return null;
        }

        OsmTrack track = routingEngine.getFoundTrack();
        int distance = routingEngine.getDistance(); // TODO add distance to result
        return asPositions(track);
    }

    private File getDirectory() {
        String directoryName = preferences.get(DIRECTORY_PREFERENCE + getName(),
                new File(System.getProperty("user.home"), ".routeconverter/brouter").getAbsolutePath());
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs())
                throw new IllegalArgumentException("Cannot create '" + getName() + "' directory '" + directory + "'");
        }
        return directory;
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

    String createFileKey(double longitude, double latitude) {
        int longitudeAsInteger = ((int) longitude / 5) * 5;
        int latitudeAsInteger = ((int) latitude / 5) * 5;
        return format("%s%d_%s%d",
                longitude < 0 ? "W" : "E",
                longitude < 0 ? -longitudeAsInteger : longitudeAsInteger,
                latitude < 0 ? "S" : "N",
                latitude < 0 ? -latitudeAsInteger : latitudeAsInteger);
    }

    private File createFile(String key) {
        return new File(getDirectory(), format("%s%s", key, ".rd5"));
    }

    public void downloadRoutingDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<String> keys = new HashSet<String>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Set<String> uris = new HashSet<String>();
        for (String key : keys) {
            File file = createFile(key);
            if (!file.exists()) {
                uris.add(key);
            }
        }

        Collection<Download> downloads = new HashSet<Download>();
        for (String uri : uris) {
            Download download = download(uri);
            if (download != null)
                downloads.add(download);
        }

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private Download download(String key) {
        String url = format("%ssegments2/%s.rd5", getBaseUrl(), key);
        File archive = createFile(key);
        return downloadManager.queueForDownloadAndProcess(getName() + " routing data for " + key, url, archive, null);
    }
}
