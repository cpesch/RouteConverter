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

package slash.navigation.converter.cmdline;

import btools.router.OsmNodeNamed;
import btools.router.OsmTrack;
import btools.router.RoutingContext;
import btools.router.RoutingEngine;
import slash.navigation.converter.cmdline.BRouterRouteLengthComputer.LegRouter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Production {@link LegRouter}: routes a single leg with BRouter's offline
 * engine against a directory of {@code .rd5} segment files, using a default
 * profile bundled with this module. This mirrors the calling convention of
 * {@code slash.navigation.brouter.BRouter#getRouteBetween} (build a
 * {@code RoutingContext} pointing at the profile, run a {@code RoutingEngine}
 * over the segments directory, read distance from the found track) but without
 * the GUI download/DataSource machinery — the analyzer runs against segments
 * that already exist on disk (specs/00055 P3 host infra).
 * <p>
 * Profile choice: {@code trekking} — BRouter's general-purpose bike/foot
 * profile. Planned routes in the RouteConverter catalog are predominantly
 * cycling and hiking tours; trekking follows both roads and paths, so it yields
 * a plausible on-road length across the widest range of catalog routes. A
 * car-only profile would refuse footpaths and fail (fall back to beeline) on
 * exactly those tours. The profile and its {@code lookups.dat} ship as
 * resources under {@code slash/navigation/converter/cmdline/brouter/} and are
 * extracted to a temporary directory on first use.
 * <p>
 * Memory stays bounded: {@code RoutingContext.memoryclass} defaults to 64 MB
 * for the node cache and a fresh engine is used per leg, well within the
 * {@code -Xmx1g} the analyzer runs with.
 *
 * @author Christian Pesch
 */
class BRouterLegRouter implements LegRouter {
    private static final Logger log = Logger.getLogger(BRouterLegRouter.class.getName());
    private static final String PROFILE_NAME = "trekking.brf";
    private static final String LOOKUPS_NAME = "lookups.dat";
    private static final String RESOURCE_PREFIX = "brouter/";
    private static final long MINIMUM_TIMEOUT = 10000L;
    private static final long MAXIMUM_TIMEOUT = 60000L;

    private final File segmentsDirectory;
    // static: one extraction per JVM, not per file — a batch-reused analyzer
    // JVM would otherwise accumulate temp directories until exit
    private static File profileFile;
    private static boolean profileExtractionAttempted;

    BRouterLegRouter(File segmentsDirectory) {
        this.segmentsDirectory = segmentsDirectory;
    }

    public Double routeLeg(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        if (segmentsDirectory == null || !segmentsDirectory.isDirectory()) {
            log.warning(format("BRouter segments directory %s does not exist; cannot route", segmentsDirectory));
            return null;
        }

        File profile = getProfileFile();
        if (profile == null)
            return null;

        RoutingContext routingContext = new RoutingContext();
        routingContext.localFunction = profile.getPath();

        List<OsmNodeNamed> waypoints = new ArrayList<>();
        waypoints.add(asOsmNodeNamed(fromLongitude, fromLatitude));
        waypoints.add(asOsmNodeNamed(toLongitude, toLatitude));

        RoutingEngine routingEngine = new RoutingEngine(null, null, segmentsDirectory, waypoints, routingContext);
        routingEngine.quite = true;
        routingEngine.doRun(timeoutFor(fromLongitude, fromLatitude, toLongitude, toLatitude));

        if (routingEngine.getErrorMessage() != null) {
            log.info(format("BRouter routing error: %s", routingEngine.getErrorMessage()));
            return null;
        }
        OsmTrack track = routingEngine.getFoundTrack();
        if (track == null)
            return null;
        return (double) routingEngine.getDistance();
    }

    /**
     * Larger legs get a longer budget, matching the heuristic in
     * {@code BRouter#getRouteBetween}, but capped so a single hostile leg can
     * never wedge a batch of analyze runs.
     */
    private long timeoutFor(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        double meanLatitude = Math.toRadians((fromLatitude + toLatitude) / 2);
        double deltaLongitude = (toLongitude - fromLongitude) * Math.cos(meanLatitude);
        double deltaLatitude = toLatitude - fromLatitude;
        double beelineMeters = Math.sqrt(deltaLongitude * deltaLongitude + deltaLatitude * deltaLatitude) * 111320.0;
        long timeout = (long) (MINIMUM_TIMEOUT + beelineMeters / 15.0);
        return Math.min(timeout, MAXIMUM_TIMEOUT);
    }

    private static synchronized File getProfileFile() {
        if (profileExtractionAttempted)
            return profileFile;
        profileExtractionAttempted = true;
        try {
            File directory = Files.createTempDirectory("brouter-analyze").toFile();
            directory.deleteOnExit();
            File extractedLookups = extractResource(LOOKUPS_NAME, directory);
            extractedLookups.deleteOnExit();
            File extractedProfile = extractResource(PROFILE_NAME, directory);
            extractedProfile.deleteOnExit();
            profileFile = extractedProfile;
        } catch (IOException e) {
            log.warning(format("Cannot extract BRouter profile; routed lengths disabled: %s", e));
            profileFile = null;
        }
        return profileFile;
    }

    private static File extractResource(String name, File directory) throws IOException {
        File target = new File(directory, name);
        try (InputStream in = BRouterLegRouter.class.getResourceAsStream(RESOURCE_PREFIX + name)) {
            if (in == null)
                throw new IOException("Resource " + RESOURCE_PREFIX + name + " not found on classpath");
            try (OutputStream out = Files.newOutputStream(target.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1)
                    out.write(buffer, 0, read);
            }
        }
        return target;
    }

    private OsmNodeNamed asOsmNodeNamed(double longitude, double latitude) {
        OsmNodeNamed result = new OsmNodeNamed();
        result.ilon = (int) ((longitude + 180.0) * 1000000.0 + 0.5);
        result.ilat = (int) ((latitude + 90.0) * 1000000.0 + 0.5);
        return result;
    }
}
