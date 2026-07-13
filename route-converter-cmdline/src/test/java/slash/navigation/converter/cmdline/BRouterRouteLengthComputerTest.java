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

import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.CmdLineNavigationFormatRegistry;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.base.RouteCharacteristics;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Exercises {@link BRouterRouteLengthComputer} through the {@code RouteRouter}
 * seam so the routed and straight-line-fallback logic are covered without real
 * {@code .rd5} segments (specs/00055 P2b). One case drives the production
 * {@link BRouterRouteRouter} against a non-existent segments directory to prove
 * the real path degrades to straight-line rather than crashing.
 */
public class BRouterRouteLengthComputerTest {

    private BaseRoute<?, ?> firstRoute(String resource) throws IOException, URISyntaxException {
        File source = new File(getClass().getResource(resource).toURI());
        NavigationFormatParser parser = new NavigationFormatParser(new CmdLineNavigationFormatRegistry());
        ParserResult result = parser.read(source);
        assertTrue(result.isSuccessful());
        return result.getAllRoutes().get(0);
    }

    @Test
    public void routeTypeRoutesWholeListInOneCallAndReportsRouted() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");
        assertEquals(RouteCharacteristics.Route, route.getCharacteristics());

        // routing must be one call over all three route points, and the routed
        // distance must be plausibly >= the straight-line
        int[] calls = {0};
        int[] pointCount = {0};
        double routedMeters = route.getDistance() + 5000.0;
        BRouterRouteLengthComputer.RouteRouter fake = (longitudes, latitudes) -> {
            calls[0]++;
            pointCount[0] = longitudes.length;
            return routedMeters;
        };
        RouteLengthComputer computer = new BRouterRouteLengthComputer(fake);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("routed", result.kind());
        assertEquals(1, calls[0]);
        assertEquals(3, pointCount[0]);
        assertEquals(routedMeters, result.meters(), 0.0001);
    }

    @Test
    public void routeTypeFallsBackToStraightLineWhenRoutingFails() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        BRouterRouteLengthComputer.RouteRouter fake = (longitudes, latitudes) -> null;
        RouteLengthComputer computer = new BRouterRouteLengthComputer(fake);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("straight-line", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void routeTypeTreatsRoutingExceptionAsFailureAndFallsBackToStraightLine() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        BRouterRouteLengthComputer.RouteRouter throwing = (longitudes, latitudes) -> {
            throw new RuntimeException("simulated routing crash");
        };
        RouteLengthComputer computer = new BRouterRouteLengthComputer(throwing);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("straight-line", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void impossiblyShortRoutedLengthIsDistrustedAndFallsBackToStraightLine() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        // a routed length far below the straight-line through the same points is
        // impossible (routing can never be shorter than the straight line), so
        // it must be distrusted and reported as straight-line
        BRouterRouteLengthComputer.RouteRouter tooShort = (longitudes, latitudes) -> 1.0;
        RouteLengthComputer computer = new BRouterRouteLengthComputer(tooShort);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("straight-line", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void trackListIsDelegatedToPointToPointAndReportsTrack() throws IOException, URISyntaxException {
        BaseRoute<?, ?> track = firstRoute("analyze-two-tracks.gpx");
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());

        // a RouteRouter that must never be consulted for a non-Route list
        BRouterRouteLengthComputer.RouteRouter forbidden = (longitudes, latitudes) -> {
            throw new AssertionError("routing must not be attempted for a Track list");
        };
        RouteLengthComputer computer = new BRouterRouteLengthComputer(forbidden);

        RouteLengthComputer.LengthResult result = computer.computeLength(track);
        assertEquals("track", result.kind());
    }

    @Test
    public void realBRouterWithoutSegmentsFallsBackToStraightLine() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        File missing = new File(System.getProperty("java.io.tmpdir"), "no-such-brouter-segments-dir");
        RouteLengthComputer computer = new BRouterRouteLengthComputer(missing);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("straight-line", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void shortRouteWithFewerThanTwoCoordinatesReturnsNull() {
        BRouterRouteLengthComputer.RouteRouter fake = (longitudes, latitudes) -> 1.0;
        RouteLengthComputer computer = new BRouterRouteLengthComputer(fake);

        BaseRoute<?, ?> empty = new slash.navigation.gpx.GpxRoute(new slash.navigation.gpx.Gpx11Format(),
                RouteCharacteristics.Route, "empty", new ArrayList<>(), new ArrayList<>());
        assertNull(computer.computeLength(empty));
    }
}
