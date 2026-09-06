/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CoverageOverlayController}'s tiling logic.
 * <p>
 * The rest of the class is coupled to {@code BaseRouteConverter#getInstance()} static
 * singletons and isn't unit-testable as-is; {@code computeTileCoverage} is pure and
 * package-private specifically so it can be covered directly here.
 *
 * @author Christian Pesch
 */

public class CoverageOverlayControllerTest {
    private final CoverageOverlayController controller = new CoverageOverlayController();

    @Test
    public void computeTileCoverageReturnsEmptyMapForNoCoveredBoxes() {
        BoundingBox viewport = boundingBox(10.0, 40.0, 12.0, 41.0);

        Map<BoundingBox, Boolean> result = controller.computeTileCoverage(viewport, emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    public void computeTileCoverageTilesTheViewportOnADegreeGrid() {
        // 2 degrees wide, 1 degree tall -- an asymmetric viewport catches a longitude/latitude
        // mixup in the tile construction the way a square one would not
        BoundingBox viewport = boundingBox(10.0, 40.0, 12.0, 41.0);
        BoundingBox coveredEverywhere = boundingBox(0.0, 0.0, 20.0, 50.0);

        Map<BoundingBox, Boolean> result = controller.computeTileCoverage(viewport, List.of(coveredEverywhere));

        assertEquals(2, result.size());
        assertEquals(Boolean.TRUE, result.get(boundingBox(10.0, 40.0, 11.0, 41.0)));
        assertEquals(Boolean.TRUE, result.get(boundingBox(11.0, 40.0, 12.0, 41.0)));
    }

    @Test
    public void computeTileCoverageOnlyMarksTilesTheCoveredBoxActuallyReaches() {
        BoundingBox viewport = boundingBox(10.0, 40.0, 12.0, 41.0);
        // only reaches the western tile (10-11); stops well short of the eastern one (11-12)
        BoundingBox coveredWestHalf = boundingBox(9.0, 39.0, 10.5, 42.0);

        Map<BoundingBox, Boolean> result = controller.computeTileCoverage(viewport, List.of(coveredWestHalf));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(boundingBox(10.0, 40.0, 11.0, 41.0)));
    }

    private BoundingBox boundingBox(double swLon, double swLat, double neLon, double neLat) {
        NavigationPosition southWest = new SimpleNavigationPosition(swLon, swLat);
        NavigationPosition northEast = new SimpleNavigationPosition(neLon, neLat);
        return new BoundingBox(northEast, southWest);
    }
}
