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
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.Polygon;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.routing.BaseRoutingService;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;
import slash.navigation.routing.TravelRestrictions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    public void computeAreaCoverageUsesThePolygonInsteadOfTheBoundingBox() {
        BoundingBox viewport = boundingBox(10.0, 40.0, 12.0, 41.0);
        BoundingBox wide = boundingBox(9.0, 39.0, 13.0, 42.0);
        // triangle that reaches the western tile but not the eastern one
        Polygon triangle = Polygon.of(List.of(List.of(position(9.0, 39.0), position(11.0, 39.0),
                position(9.0, 42.0), position(9.0, 39.0))), List.of(false));

        Map<BoundingBox, Boolean> result = controller.computeAreaCoverage(viewport,
                List.of(new CoverageOverlayController.CoveredArea(wide, triangle)));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(boundingBox(10.0, 40.0, 11.0, 41.0)));
    }

    @Test
    public void computeAreaCoverageFallsBackToTheBoundingBoxWithoutAPolygon() {
        BoundingBox viewport = boundingBox(10.0, 40.0, 12.0, 41.0);
        BoundingBox wide = boundingBox(9.0, 39.0, 13.0, 42.0);

        Map<BoundingBox, Boolean> result = controller.computeAreaCoverage(viewport,
                List.of(new CoverageOverlayController.CoveredArea(wide, null)));

        assertEquals(2, result.size());
    }

    private static NavigationPosition position(double longitude, double latitude) {
        return new SimpleNavigationPosition(longitude, latitude);
    }

    // rc#178: mirrors CoverageOverlayController#computeRoutingCoverageByMap's use of the
    // RoutingService.isRoutingDataAvailable() seam to decide which candidate maps are covered.
    // That method itself is coupled to BaseRouteConverter#getInstance() (via getCoverageCandidateMaps())
    // and can't be exercised directly, so this drives computeAreaCoverage() -- which is what it
    // ultimately feeds -- through the same association, using a stub RoutingService instead.
    @Test
    public void computeAreaCoverageOnlyIncludesMapsTheRoutingServiceReportsAsAvailable() {
        BoundingBox viewport = boundingBox(10.0, 40.0, 12.0, 41.0);
        // stops short of 11.0 -- an edge exactly on the tile seam still intersects the east tile too,
        // since BoundingBox#intersect() treats a touching, zero-width overlap as an intersection
        MapDescriptor availableMap = mapDescriptor(boundingBox(9.0, 39.0, 10.9, 42.0));
        MapDescriptor unavailableMap = mapDescriptor(boundingBox(11.0, 39.0, 13.0, 42.0));
        RoutingService routingService = new StubRoutingService(Set.of(availableMap));

        List<CoverageOverlayController.CoveredArea> covered = new ArrayList<>();
        for (MapDescriptor mapDescriptor : List.of(availableMap, unavailableMap)) {
            if (routingService.isRoutingDataAvailable(mapDescriptor))
                covered.add(new CoverageOverlayController.CoveredArea(mapDescriptor.getBoundingBox(), null));
        }

        Map<BoundingBox, Boolean> result = controller.computeAreaCoverage(viewport, covered);

        assertEquals(1, result.size());
        assertTrue(result.containsKey(boundingBox(10.0, 40.0, 11.0, 41.0)));
    }

    private static MapDescriptor mapDescriptor(BoundingBox boundingBox) {
        return new MapDescriptor() {
            public String getIdentifier() {
                return "test";
            }

            public BoundingBox getBoundingBox() {
                return boundingBox;
            }
        };
    }

    private static class StubRoutingService extends BaseRoutingService {
        private final Set<MapDescriptor> available;

        StubRoutingService(Set<MapDescriptor> available) {
            this.available = available;
        }

        public String getName() {
            return "Stub";
        }

        public boolean isInitialized() {
            return true;
        }

        public boolean isDownload() {
            return true;
        }

        public List<TravelMode> getAvailableTravelModes() {
            throw new UnsupportedOperationException();
        }

        public TravelRestrictions getAvailableTravelRestrictions() {
            throw new UnsupportedOperationException();
        }

        public TravelMode getPreferredTravelMode() {
            throw new UnsupportedOperationException();
        }

        public String getPath() {
            throw new UnsupportedOperationException();
        }

        public void setPath(String path) {
            throw new UnsupportedOperationException();
        }

        public RoutingResult getRouteBetween(NavigationPosition from, NavigationPosition to, TravelMode travelMode, TravelRestrictions travelRestrictions) {
            throw new UnsupportedOperationException();
        }

        public NavigationPosition getSnapToRoadPosition(NavigationPosition position) {
            throw new UnsupportedOperationException();
        }

        public DownloadFuture downloadRoutingDataFor(String mapIdentifier, List<LongitudeAndLatitude> longitudeAndLatitudes) {
            throw new UnsupportedOperationException();
        }

        public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
            throw new UnsupportedOperationException();
        }

        public void downloadRoutingData(List<MapDescriptor> mapDescriptors) {
            throw new UnsupportedOperationException();
        }

        public Map<BoundingBox, Boolean> getCoverageTiles(BoundingBox area) {
            return Map.of();
        }

        public boolean isRoutingDataAvailable(MapDescriptor mapDescriptor) {
            return available.contains(mapDescriptor);
        }
    }
}
