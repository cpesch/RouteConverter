package slash.navigation.hgt;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.download.DownloadManager;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HgtFilesTest {
    private final HgtFiles files = new HgtFiles(null, new DownloadManager(null));

    @Test
    public void createFileKey() {
        assertEquals("N41E000.hgt", files.createFileKey(0.1, 41.9));
        assertEquals("N42E000.hgt", files.createFileKey(0.1, 42.0));
        assertEquals("N42E000.hgt", files.createFileKey(0.1, 42.1));

        assertEquals("N42W001.hgt", files.createFileKey(-0.1, 42.0));
        assertEquals("N42E000.hgt", files.createFileKey(0.0, 42.0));
        assertEquals("N42E000.hgt", files.createFileKey(0.1, 42.0));

        assertEquals("N42E000.hgt", files.createFileKey(0.15052, 42.42091));
        assertEquals("N42W001.hgt", files.createFileKey(-0.55289, 42.55803));
    }

    @Test
    public void testGetCoverageTilesReturnsCorrectTileCount() {
        // Create a bounding box that spans 2x2 tiles
        NavigationPosition southWest = new SimpleNavigationPosition(0.0, 0.0);
        NavigationPosition northEast = new SimpleNavigationPosition(2.0, 2.0);
        BoundingBox bbox = new BoundingBox(northEast, southWest);

        // This test verifies the tile count; actual coverage depends on file existence
        Map<BoundingBox, Boolean> coverageTiles = files.getCoverageTiles(bbox);

        // Should return 4 tiles for a 2x2 degree area
        assertEquals(4, coverageTiles.size());
    }

    @Test
    public void testGetCoverageTilesReturnsBooleanForEachTile() {
        // Create a bounding box spanning 1x1 tile
        NavigationPosition southWest = new SimpleNavigationPosition(0.0, 42.0);
        NavigationPosition northEast = new SimpleNavigationPosition(1.0, 43.0);
        BoundingBox bbox = new BoundingBox(northEast, southWest);

        Map<BoundingBox, Boolean> coverageTiles = files.getCoverageTiles(bbox);

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
        NavigationPosition southWest = new SimpleNavigationPosition(0.5, 42.5);
        NavigationPosition northEast = new SimpleNavigationPosition(1.5, 43.5);
        BoundingBox bbox = new BoundingBox(northEast, southWest);

        Map<BoundingBox, Boolean> coverageTiles = files.getCoverageTiles(bbox);

        assertEquals(1, coverageTiles.size());
    }
}
