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
package slash.navigation.mapview.mapsforge.overlays;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.layer.Layer;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE;

/**
 * Tests for CoverageOverlay layer management.
 *
 * @author Christian Pesch
 */
public class CoverageOverlayTest {
    private Map<BoundingBox, Boolean> coverageTiles;
    private BoundingBox testBoundingBox;

    @Before
    public void setUp() {
        coverageTiles = new HashMap<>();
        testBoundingBox = createBoundingBox(0.0, 0.0, 10.0, 10.0);

        // Add some test coverage tiles
        BoundingBox coveredTile = createBoundingBox(0.0, 0.0, 1.0, 1.0);
        BoundingBox missingTile = createBoundingBox(1.0, 0.0, 2.0, 1.0);
        coverageTiles.put(coveredTile, true);  // covered
        coverageTiles.put(missingTile, false); // missing
    }

    @Test
    public void createsOverlayWithCoverageData() {
        CoverageOverlay overlay = new CoverageOverlay(coverageTiles, INSTANCE, 256);

        // Verify overlay was created successfully
        assert overlay != null;
    }

    @Test
    public void createsOverlayWithEmptyCoverage() {
        Map<BoundingBox, Boolean> emptyTiles = new HashMap<>();
        CoverageOverlay overlay = new CoverageOverlay(emptyTiles, INSTANCE, 256);

        // Verify overlay was created even with empty coverage
        assert overlay != null;
    }

    @Test
    public void createsOverlayWithAllCoveredTiles() {
        Map<BoundingBox, Boolean> allCovered = new HashMap<>();
        BoundingBox tile1 = createBoundingBox(0.0, 0.0, 1.0, 1.0);
        BoundingBox tile2 = createBoundingBox(1.0, 0.0, 2.0, 1.0);
        allCovered.put(tile1, true);
        allCovered.put(tile2, true);

        CoverageOverlay overlay = new CoverageOverlay(allCovered, INSTANCE, 256);

        // Verify overlay was created
        assert overlay != null;
    }

    @Test
    public void createsOverlayWithAllMissingTiles() {
        Map<BoundingBox, Boolean> allMissing = new HashMap<>();
        BoundingBox tile1 = createBoundingBox(0.0, 0.0, 1.0, 1.0);
        BoundingBox tile2 = createBoundingBox(1.0, 0.0, 2.0, 1.0);
        allMissing.put(tile1, false);
        allMissing.put(tile2, false);

        CoverageOverlay overlay = new CoverageOverlay(allMissing, INSTANCE, 256);

        // Verify overlay was created
        assert overlay != null;
    }

    @Test
    public void drawNeverCallsCanvasForAllMissingTiles() {
        Map<BoundingBox, Boolean> allMissing = new HashMap<>();
        allMissing.put(createBoundingBox(0.0, 0.0, 1.0, 1.0), false);
        CoverageOverlay overlay = new CoverageOverlay(allMissing, INSTANCE, 256);

        Canvas canvas = mock(Canvas.class);
        overlay.draw(null, (byte) 5, canvas, new Point(0, 0), Rotation.NULL_ROTATION);

        verify(canvas, never()).drawLine(anyInt(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    public void drawCallsCanvasForCoveredTiles() {
        Map<BoundingBox, Boolean> allCovered = new HashMap<>();
        allCovered.put(createBoundingBox(0.0, 0.0, 1.0, 1.0), true);
        CoverageOverlay overlay = new CoverageOverlay(allCovered, INSTANCE, 256);

        Canvas canvas = mock(Canvas.class);
        overlay.draw(null, (byte) 5, canvas, new Point(0, 0), Rotation.NULL_ROTATION);

        verify(canvas, atLeastOnce()).drawLine(anyInt(), anyInt(), anyInt(), anyInt(), any());
    }

    // regression test for the green-only rendering: a missing tile mixed in with a covered one
    // must contribute zero draw calls of its own, not just a different color
    @Test
    public void drawIgnoresMissingTilesEvenWhenMixedWithCovered() {
        Canvas mixedCanvas = mock(Canvas.class);
        CoverageOverlay mixed = new CoverageOverlay(coverageTiles, INSTANCE, 256); // one covered, one missing (setUp)
        mixed.draw(null, (byte) 5, mixedCanvas, new Point(0, 0), Rotation.NULL_ROTATION);

        Canvas coveredOnlyCanvas = mock(Canvas.class);
        Map<BoundingBox, Boolean> coveredOnly = new HashMap<>();
        coveredOnly.put(createBoundingBox(0.0, 0.0, 1.0, 1.0), true); // the same covered tile as in setUp
        CoverageOverlay overlay = new CoverageOverlay(coveredOnly, INSTANCE, 256);
        overlay.draw(null, (byte) 5, coveredOnlyCanvas, new Point(0, 0), Rotation.NULL_ROTATION);

        assertEquals(mockingDetails(coveredOnlyCanvas).getInvocations().size(),
                mockingDetails(mixedCanvas).getInvocations().size());
    }

    private BoundingBox createBoundingBox(double swLon, double swLat, double neLon, double neLat) {
        NavigationPosition southWest = new SimpleNavigationPosition(swLon, swLat);
        NavigationPosition northEast = new SimpleNavigationPosition(neLon, neLat);
        return new BoundingBox(northEast, southWest);
    }
}