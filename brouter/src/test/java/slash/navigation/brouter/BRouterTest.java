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
import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BRouterTest {
    private final BRouter router = new BRouter(null);

    @Before
    public void setUp() {
        DataSource profiles = mock(DataSource.class);
        when(profiles.getDirectory()).thenReturn("brouter-profiles-test");
        DataSource segments = mock(DataSource.class);
        when(segments.getDirectory()).thenReturn("brouter-segments-test");
        router.setProfilesAndSegments(profiles, segments);
    }

    @Test
    public void testLongitude() {
        assertEquals(190032145, router.asLongitude(10.032145));
        assertEquals(10.032145, router.asLongitude(190032145), 0.000001);
    }

    @Test
    public void testLatitude() {
        assertEquals(143569481, router.asLatitude(53.569481));
        assertEquals(53.569481, router.asLatitude(143569481), 0.000001);
    }

    @Test
    public void testCreateFileKey() {
        assertEquals("E0_N0.rd5", router.createFileKey(0.1, 0.1));
        assertEquals("E0_S5.rd5", router.createFileKey(0.1, -0.1));
        assertEquals("W5_N0.rd5", router.createFileKey(-0.1, 0.1));
        assertEquals("W5_S5.rd5", router.createFileKey(-0.1, -0.1));

        assertEquals("W5_N40.rd5", router.createFileKey(-4.036, 42.486));
        assertEquals("E5_N5.rd5", router.createFileKey(5.1, 9.9));
        assertEquals("E50_N50.rd5", router.createFileKey(50.1, 54.9));
        assertEquals("E175_N85.rd5", router.createFileKey(179.9, 89.9));
        assertEquals("W10_S10.rd5", router.createFileKey(-5.1, -9.9));
        assertEquals("W5_S45.rd5", router.createFileKey(-4.036, -43.431));
        assertEquals("W55_S55.rd5", router.createFileKey(-50.1, -54.9));

        assertEquals("E175_N85.rd5", router.createFileKey(179.9, 89.9));
        assertEquals("E175_S90.rd5", router.createFileKey(179.9, -89.9));
        assertEquals("W180_N85.rd5", router.createFileKey(-179.9, 89.9));
        assertEquals("W180_S90.rd5", router.createFileKey(-179.9, -89.9));
    }

    @Test
    public void testCreateFileKeys() {
        assertEquals(new HashSet<>(asList("E5_N45.rd5", "E10_N45.rd5")), router.createFileKeys(9.9859064, 49.7386072));
        assertEquals(new HashSet<>(asList("E5_N45.rd5", "E10_N45.rd5")), router.createFileKeys(9.988344, 49.7386959));
    }

    @Test
    public void testGetPreferredTravelModeIsCarEco() {
        assertEquals("car-eco", router.getPreferredTravelMode().name());
    }

    @Test
    public void testCreateWaypointsWithNullDescriptionDoesNotProduceNullName() {
        // a freshly added position (before reverse geocoding resolves a description) has description == null
        SimpleNavigationPosition from = new SimpleNavigationPosition(-3.629092777185068, 37.13627094135279);
        SimpleNavigationPosition to = new SimpleNavigationPosition(-3.6289927771850676, 37.136370941352794);

        List<OsmNodeNamed> waypoints = router.createWaypoints(from, to);

        assertEquals(2, waypoints.size());
        for (OsmNodeNamed waypoint : waypoints) {
            assertNotNull(waypoint.name);
        }
    }

    @Test
    public void testGetCoverageTilesReturnsCorrectTileCount() {
        // Create a bounding box that spans 2x2 tiles
        NavigationPosition southWest = new SimpleNavigationPosition(0.0, 0.0);
        NavigationPosition northEast = new SimpleNavigationPosition(2.0, 2.0);
        BoundingBox bbox = new BoundingBox(northEast, southWest);

        // This test verifies the tile count; actual coverage depends on file existence
        Map<BoundingBox, Boolean> coverageTiles = router.getCoverageTiles(bbox);

        // Should return 4 tiles for a 2x2 degree area
        assertEquals(4, coverageTiles.size());
    }

    @Test
    public void testGetCoverageTilesReturnsBooleanForEachTile() {
        // Create a bounding box spanning 1x1 tile
        NavigationPosition southWest = new SimpleNavigationPosition(0.0, 0.0);
        NavigationPosition northEast = new SimpleNavigationPosition(1.0, 1.0);
        BoundingBox bbox = new BoundingBox(northEast, southWest);

        Map<BoundingBox, Boolean> coverageTiles = router.getCoverageTiles(bbox);

        // Should return 1 tile with a boolean value
        assertEquals(1, coverageTiles.size());
        // The value should be either true (covered) or false (missing)
        Boolean coverage = coverageTiles.values().iterator().next();
        assertNotNull(coverage);
        assertTrue(coverage == true || coverage == false);
    }

    @Test
    public void testGetCoverageTilesHandlesPartialOverlap() {
        // Tiles are chunked 1x1 starting at the bounding box's own corner, not aligned to a fixed
        // degree grid, so a 1x1 degree area yields exactly one tile regardless of its offset.
        NavigationPosition southWest = new SimpleNavigationPosition(0.5, 0.5);
        NavigationPosition northEast = new SimpleNavigationPosition(1.5, 1.5);
        BoundingBox bbox = new BoundingBox(northEast, southWest);

        Map<BoundingBox, Boolean> coverageTiles = router.getCoverageTiles(bbox);

        assertEquals(1, coverageTiles.size());
    }
}
