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
package slash.navigation.pois.mapsforge;

import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static slash.navigation.maps.mapsforge.MapType.MBTiles;
import static slash.navigation.maps.mapsforge.MapType.Mapsforge;

public class MapsforgeMapGeocodingServiceTest {
    private static final BoundingBox MAP_BOUNDS = new BoundingBox(14.0, 53.0, 13.0, 52.0);
    private static final BoundingBox VISIBLE_BOUNDS = new BoundingBox(13.60, 52.60, 13.30, 52.30);
    private static final NavigationPosition CENTER = MAP_BOUNDS.getCenter();

    @Test
    public void returnsNullWhenNoDisplayedReadableMapsforgeMapExists() throws Exception {
        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(null), () -> VISIBLE_BOUNDS);

        assertNull(service.getPositionsFor("Berlin"));
        assertNull(service.getAddressFor(CENTER));
    }

    @Test
    public void returnsEmptyListForBlankSearchString() throws Exception {
        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(null), () -> VISIBLE_BOUNDS);

        List<GeocodingResult> results = service.getPositionsFor("   ");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void returnsNullWhenDisplayedMapTypeIsNotMapsforge() throws Exception {
        MapsforgeFileMap displayedMap = mockDisplayedMap();
        when(displayedMap.getType()).thenReturn(MBTiles);
        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(displayedMap), () -> VISIBLE_BOUNDS);

        assertNull(service.getPositionsFor("Berlin"));
        assertNull(service.getAddressFor(CENTER));
        verify(displayedMap, never()).getMapFile();
    }

    @Test
    public void prefersVisibleBoundsAndFallsBackToFullMapBounds() throws Exception {
        MapsforgeFileMap displayedMap = mockDisplayedMap();
        AtomicInteger invocations = new AtomicInteger();
        when(displayedMap.getMapFile().readNamedItems(any(Tile.class), any(Tile.class))).thenAnswer(invocation -> {
            if (invocations.getAndIncrement() == 0)
                return readResult();
            return readResult(poi(13.80, 52.80, new Tag("name", "Outside View"), new Tag("place", "village")));
        });

        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(displayedMap), () -> VISIBLE_BOUNDS);
        List<GeocodingResult> results = service.getPositionsFor("Outside");

        assertEquals(2, invocations.get());
        assertEquals(1, results.size());
        assertEquals("Outside View (village)", results.get(0).position().getDescription());
        assertEquals("Mapsforge Map", results.get(0).geocodingServiceName());
    }

    @Test
    public void matchesMultilingualNamesAndUsefulCategoryTagsButIgnoresAddressOnlyNoise() throws Exception {
        MapsforgeFileMap displayedMap = mockDisplayedMap();
        when(displayedMap.getMapFile().readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(
                poi(13.4050, 52.5200, new Tag("name", "Prague"), new Tag("name:cs", "Praha"), new Tag("place", "city")),
                poi(13.4100, 52.5210, new Tag("amenity", "fuel")),
                poi(13.4110, 52.5220, new Tag("addr:street", "Main Street"), new Tag("addr:housenumber", "1"))
        ));

        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(displayedMap), () -> VISIBLE_BOUNDS);

        List<GeocodingResult> multilingual = service.getPositionsFor("Praha");
        assertEquals(1, multilingual.size());
        assertEquals("Praha (city)", multilingual.get(0).position().getDescription());

        List<GeocodingResult> category = service.getPositionsFor("fuel");
        assertEquals(1, category.size());
        assertEquals("fuel", category.get(0).position().getDescription());

        List<GeocodingResult> ignored = service.getPositionsFor("Main Street");
        assertNotNull(ignored);
        assertTrue(ignored.isEmpty());
    }

    @Test
    public void sortsByDistanceAndLimitsResults() throws Exception {
        MapsforgeFileMap displayedMap = mockDisplayedMap();
        List<PointOfInterest> pois = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double offset = i * 0.001;
            pois.add(poi(CENTER.getLongitude() + offset, CENTER.getLatitude() + offset, new Tag("name", "Test " + i)));
        }
        when(displayedMap.getMapFile().readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(pois.toArray(new PointOfInterest[0])));

        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(displayedMap), () -> MAP_BOUNDS);
        List<GeocodingResult> results = service.getPositionsFor("test");

        assertEquals(50, results.size());
        assertEquals("Test 0", results.get(0).position().getDescription());
        assertEquals("Test 49", results.get(49).position().getDescription());
    }

    @Test
    public void reverseLookupReturnsNearestNamedFeatureWithinRadius() throws Exception {
        MapsforgeFileMap displayedMap = mockDisplayedMap();
        when(displayedMap.getMapFile().readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(
                poi(13.4005, 52.5005, new Tag("name", "Near Place"), new Tag("place", "hamlet")),
                poi(13.4060, 52.5060, new Tag("name", "Farther Place"), new Tag("place", "hamlet"))
        ));

        MapsforgeMapGeocodingService service = new MapsforgeMapGeocodingService(mockMapManager(displayedMap), () -> VISIBLE_BOUNDS);
        String address = service.getAddressFor(new SimpleNavigationPosition(13.4000, 52.5000));

        assertEquals("Near Place (hamlet)", address);
    }

    private MapsforgeFileMap mockDisplayedMap() {
        MapsforgeFileMap displayedMap = mock(MapsforgeFileMap.class);
        org.mapsforge.map.reader.MapFile mapFile = mock(org.mapsforge.map.reader.MapFile.class);
        when(displayedMap.getType()).thenReturn(Mapsforge);
        when(displayedMap.getMapFile()).thenReturn(mapFile);
        when(displayedMap.getBoundingBox()).thenReturn(MAP_BOUNDS);
        return displayedMap;
    }

    @SuppressWarnings("unchecked")
    private MapsforgeMapManager mockMapManager(LocalMap localMap) {
        MapsforgeMapManager mapsforgeMapManager = mock(MapsforgeMapManager.class);
        ItemModel<LocalMap> displayedMapModel = mock(ItemModel.class);
        when(mapsforgeMapManager.getDisplayedMapModel()).thenReturn(displayedMapModel);
        when(displayedMapModel.getItem()).thenReturn(localMap);
        return mapsforgeMapManager;
    }

    private MapReadResult readResult(PointOfInterest... pois) {
        MapReadResult result = new MapReadResult();
        result.pois.addAll(asList(pois));
        return result;
    }

    private PointOfInterest poi(double longitude, double latitude, Tag... tags) {
        return new PointOfInterest((byte) 0, asList(tags), new LatLong(latitude, longitude));
    }
}

