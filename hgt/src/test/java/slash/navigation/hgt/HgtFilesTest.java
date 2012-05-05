package slash.navigation.hgt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HgtFilesTest {
    private HgtFiles files = new HgtFiles();

    @Test
    public void createTileKey() {
        assertEquals(new Integer(0), files.createTileKey(-180.0, -90.0));
        assertEquals(new Integer(180), files.createTileKey(0.0, -90.0));
        assertEquals(new Integer(360), files.createTileKey(180.0, -90.0));
        assertEquals(new Integer(9000000), files.createTileKey(-180.0, 0.0));
        assertEquals(new Integer(9000180), files.createTileKey(0.0, 0.0));
        assertEquals(new Integer(9000360), files.createTileKey(180.0, 0.0));
        assertEquals(new Integer(18000000), files.createTileKey(-180.0, 90.0));
        assertEquals(new Integer(18000180), files.createTileKey(0.0, 90.0));
        assertEquals(new Integer(18000360), files.createTileKey(180.0, 90.0));

        assertEquals(new Integer(13200180), files.createTileKey(0.15052, 42.42091));
        assertEquals(new Integer(13200179), files.createTileKey(-0.55289, 42.55803));
    }

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
}
