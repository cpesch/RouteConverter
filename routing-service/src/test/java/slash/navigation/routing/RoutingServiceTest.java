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

package slash.navigation.routing;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link RoutingService#isRoutingDataAvailable(MapDescriptor)} default, which derives
 * availability from the {@link RoutingService#getCoverageTiles(BoundingBox)} grid -- the
 * implementation every tile-grid routing service ships with (e.g. BRouter), while services
 * without a tile grid (GraphHopper) override it.
 *
 * @author Christian Pesch
 */

public class RoutingServiceTest {
    private final MapDescriptor mapDescriptor = new MapDescriptor() {
        public String getIdentifier() {
            return "test-map";
        }

        public BoundingBox getBoundingBox() {
            return boundingBox(0.0, 0.0, 2.0, 1.0);
        }
    };

    // rc#178: the empty grid case is the one that must answer false -- nothing published for the
    // region at all is not the same as everything for the region already downloaded
    @Test
    public void isRoutingDataAvailableIsFalseForAnEmptyCoverageGrid() {
        assertFalse("an empty coverage grid means nothing is published, not that everything is downloaded",
                new TileGridRoutingService(Map.of()).isRoutingDataAvailable(mapDescriptor));
    }

    @Test
    public void isRoutingDataAvailableIsFalseWhenACoverageTileIsMissing() {
        Map<BoundingBox, Boolean> grid = new HashMap<>();
        grid.put(boundingBox(0.0, 0.0, 1.0, 1.0), true);
        grid.put(boundingBox(1.0, 0.0, 2.0, 1.0), false);

        assertFalse(new TileGridRoutingService(grid).isRoutingDataAvailable(mapDescriptor));
    }

    @Test
    public void isRoutingDataAvailableIsTrueWhenAllCoverageTilesArePresent() {
        Map<BoundingBox, Boolean> grid = new HashMap<>();
        grid.put(boundingBox(0.0, 0.0, 1.0, 1.0), true);
        grid.put(boundingBox(1.0, 0.0, 2.0, 1.0), true);

        assertTrue(new TileGridRoutingService(grid).isRoutingDataAvailable(mapDescriptor));
    }

    @Test
    public void isRoutingDataAvailableQueriesTheGridOfTheMapBoundingBox() {
        TileGridRoutingService routingService = new TileGridRoutingService(Map.of());
        routingService.isRoutingDataAvailable(mapDescriptor);

        assertEquals(mapDescriptor.getBoundingBox(), routingService.getQueriedArea());
    }

    private static BoundingBox boundingBox(double west, double south, double east, double north) {
        NavigationPosition southWest = new SimpleNavigationPosition(west, south);
        NavigationPosition northEast = new SimpleNavigationPosition(east, north);
        return new BoundingBox(northEast, southWest);
    }

    private static class TileGridRoutingService extends BaseRoutingService {
        private final Map<BoundingBox, Boolean> coverageTiles;
        private BoundingBox queriedArea;

        TileGridRoutingService(Map<BoundingBox, Boolean> coverageTiles) {
            this.coverageTiles = coverageTiles;
        }

        BoundingBox getQueriedArea() {
            return queriedArea;
        }

        public String getName() {
            return "TileGrid";
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
            queriedArea = area;
            return coverageTiles;
        }
    }
}
