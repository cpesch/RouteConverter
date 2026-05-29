package slash.navigation.pois.mapsforge;

import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

public class MapsforgeMapLookupTest {
    private static final BoundingBox MAP_BOUNDS = new BoundingBox(14.0, 53.0, 13.0, 52.0);
    private static final BoundingBox VISIBLE_BOUNDS = new BoundingBox(13.60, 52.60, 13.30, 52.30);
    private static final NavigationPosition CENTER = MAP_BOUNDS.getCenter();

    @Test
    public void returnsEmptyListWhenBoundsAreNull() {
        MapFile mapFile = mock(MapFile.class);
        MapsforgeMapLookup lookup = new MapsforgeMapLookup();

        List<NavigationPosition> results = lookup.search(mapFile, normalize("Berlin"), null, CENTER);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(mapFile);
    }

    @Test
    public void searchesNamesNameVariantsAndCategoriesButIgnoresAddressOnlyTags() {
        MapFile mapFile = mock(MapFile.class);
        when(mapFile.readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(
                poi(13.4050, 52.5200, new Tag("name", "Prague"), new Tag("name:cs", "Praha"), new Tag("place", "city")),
                poi(13.4100, 52.5210, new Tag("amenity", "fuel")),
                poi(13.4110, 52.5220, new Tag("addr:street", "Main Street"), new Tag("addr:housenumber", "1"))
        ));
        MapsforgeMapLookup lookup = new MapsforgeMapLookup();

        List<NavigationPosition> multilingual = lookup.search(mapFile, normalize("Praha"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, multilingual.size());
        assertEquals("Praha (city)", multilingual.get(0).getDescription());

        List<NavigationPosition> category = lookup.search(mapFile, normalize("fuel"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, category.size());
        assertEquals("fuel", category.get(0).getDescription());

        assertTrue(lookup.search(mapFile, normalize("Main Street"), VISIBLE_BOUNDS, CENTER).isEmpty());
    }

    @Test
    public void sortsByDistanceAndLimitsResults() {
        MapFile mapFile = mock(MapFile.class);
        List<PointOfInterest> pois = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            double offset = i * 0.001;
            pois.add(poi(CENTER.getLongitude() + offset, CENTER.getLatitude() + offset, new Tag("name", "Test " + i)));
        }
        when(mapFile.readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(pois.toArray(new PointOfInterest[0])));
        MapsforgeMapLookup lookup = new MapsforgeMapLookup();

        List<NavigationPosition> results = lookup.search(mapFile, normalize("test"), MAP_BOUNDS, CENTER);

        assertEquals(50, results.size());
        assertEquals("Test 0", results.get(0).getDescription());
        assertEquals("Test 49", results.get(49).getDescription());
    }

    @Test
    public void reverseLookupReturnsNearestNamedFeatureWithinRadius() {
        MapFile mapFile = mock(MapFile.class);
        when(mapFile.readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(
                poi(13.4005, 52.5005, new Tag("name", "Near Place"), new Tag("place", "hamlet")),
                poi(13.4060, 52.5060, new Tag("name", "Farther Place"), new Tag("place", "hamlet"))
        ));
        MapsforgeMapLookup lookup = new MapsforgeMapLookup();

        String address = lookup.lookup(mapFile, new SimpleNavigationPosition(13.4000, 52.5000));

        assertEquals("Near Place (hamlet)", address);
    }

    @Test
    public void usesWayLabelPositionBeforeWayGeometry() {
        MapFile mapFile = mock(MapFile.class);
        LatLong geometryPoint = new LatLong(52.1000, 13.1000);
        LatLong labelPosition = new LatLong(52.5200, 13.4050);
        when(mapFile.readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(way(
                new LatLong[][]{{geometryPoint}}, labelPosition, new Tag("name", "Label Way"), new Tag("highway", "residential")
        )));
        MapsforgeMapLookup lookup = new MapsforgeMapLookup();

        List<NavigationPosition> results = lookup.search(mapFile, normalize("Label"), VISIBLE_BOUNDS, CENTER);

        assertEquals(1, results.size());
        assertEquals(labelPosition.longitude, results.get(0).getLongitude(), 0.0);
        assertEquals(labelPosition.latitude, results.get(0).getLatitude(), 0.0);
        assertEquals("Label Way (residential)", results.get(0).getDescription());
    }

    @Test
    public void fallsBackToFirstWayGeometryPoint() {
        MapFile mapFile = mock(MapFile.class);
        LatLong geometryPoint = new LatLong(52.5200, 13.4050);
        when(mapFile.readNamedItems(any(Tile.class), any(Tile.class))).thenReturn(readResult(way(
                new LatLong[][]{{geometryPoint}}, null, new Tag("name", "Geometry Way"), new Tag("highway", "service")
        )));
        MapsforgeMapLookup lookup = new MapsforgeMapLookup();

        List<NavigationPosition> results = lookup.search(mapFile, normalize("Geometry"), VISIBLE_BOUNDS, CENTER);

        assertEquals(1, results.size());
        assertEquals(geometryPoint.longitude, results.get(0).getLongitude(), 0.0);
        assertEquals(geometryPoint.latitude, results.get(0).getLatitude(), 0.0);
        assertEquals("Geometry Way (service)", results.get(0).getDescription());
    }

    private MapReadResult readResult(PointOfInterest... pois) {
        MapReadResult result = new MapReadResult();
        result.pois.addAll(asList(pois));
        return result;
    }

    private MapReadResult readResult(Way... ways) {
        MapReadResult result = new MapReadResult();
        result.ways.addAll(asList(ways));
        return result;
    }

    private PointOfInterest poi(double longitude, double latitude, Tag... tags) {
        return new PointOfInterest((byte) 0, asList(tags), new LatLong(latitude, longitude));
    }

    private Way way(LatLong[][] latLongs, LatLong labelPosition, Tag... tags) {
        return new Way((byte) 0, asList(tags), latLongs, labelPosition);
    }
}

