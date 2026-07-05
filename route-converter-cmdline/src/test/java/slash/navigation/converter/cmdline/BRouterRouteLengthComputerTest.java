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
 * Exercises {@link BRouterRouteLengthComputer} through the {@code LegRouter}
 * seam so the routed-summation and beeline-fallback logic are covered without
 * real {@code .rd5} segments (specs/00055 P2b). One case drives the production
 * {@link BRouterLegRouter} against a non-existent segments directory to prove
 * the real path degrades to beeline rather than crashing.
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
    public void routeTypeSumsRoutedLegDistancesAndReportsRouted() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");
        assertEquals(RouteCharacteristics.Route, route.getCharacteristics());

        List<Object[]> legs = new ArrayList<>();
        BRouterRouteLengthComputer.LegRouter fake = (fromLon, fromLat, toLon, toLat) -> {
            legs.add(new Object[]{fromLon, fromLat, toLon, toLat});
            return 1234.0;
        };
        RouteLengthComputer computer = new BRouterRouteLengthComputer(fake);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("routed", result.kind());
        // three route points -> two routed legs -> 2 * 1234
        assertEquals(2, legs.size());
        assertEquals(2468.0, result.meters(), 0.0001);
    }

    @Test
    public void routeTypeFallsBackToBeelineWhenALegCannotBeRouted() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        // fail the second leg only
        int[] calls = {0};
        BRouterRouteLengthComputer.LegRouter fake = (fromLon, fromLat, toLon, toLat) ->
                (++calls[0] == 1) ? 1000.0 : null;
        RouteLengthComputer computer = new BRouterRouteLengthComputer(fake);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("beeline", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void routeTypeTreatsRoutingExceptionAsLegFailureAndFallsBackToBeeline() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        BRouterRouteLengthComputer.LegRouter throwing = (fromLon, fromLat, toLon, toLat) -> {
            throw new RuntimeException("simulated routing crash");
        };
        RouteLengthComputer computer = new BRouterRouteLengthComputer(throwing);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("beeline", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void trackListIsDelegatedToPointToPointAndReportsTrack() throws IOException, URISyntaxException {
        BaseRoute<?, ?> track = firstRoute("analyze-two-tracks.gpx");
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());

        // a LegRouter that must never be consulted for a non-Route list
        BRouterRouteLengthComputer.LegRouter forbidden = (fromLon, fromLat, toLon, toLat) -> {
            throw new AssertionError("routing must not be attempted for a Track list");
        };
        RouteLengthComputer computer = new BRouterRouteLengthComputer(forbidden);

        RouteLengthComputer.LengthResult result = computer.computeLength(track);
        assertEquals("track", result.kind());
    }

    @Test
    public void realBRouterWithoutSegmentsFallsBackToBeeline() throws IOException, URISyntaxException {
        BaseRoute<?, ?> route = firstRoute("analyze-route.gpx");

        File missing = new File(System.getProperty("java.io.tmpdir"), "no-such-brouter-segments-dir");
        RouteLengthComputer computer = new BRouterRouteLengthComputer(missing);

        RouteLengthComputer.LengthResult result = computer.computeLength(route);
        assertEquals("beeline", result.kind());
        assertEquals(route.getDistance(), result.meters(), 0.0001);
    }

    @Test
    public void shortRouteWithFewerThanTwoCoordinatesReturnsNull() {
        BRouterRouteLengthComputer.LegRouter fake = (fromLon, fromLat, toLon, toLat) -> 1.0;
        RouteLengthComputer computer = new BRouterRouteLengthComputer(fake);

        BaseRoute<?, ?> empty = new slash.navigation.gpx.GpxRoute(new slash.navigation.gpx.Gpx11Format(),
                RouteCharacteristics.Route, "empty", new ArrayList<>(), new ArrayList<>());
        assertNull(computer.computeLength(empty));
    }
}
