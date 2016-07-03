package slash.navigation.hgt;

import org.junit.Test;
import slash.navigation.download.DownloadManager;

import static org.junit.Assert.assertEquals;

public class HgtFilesTest {
    private HgtFiles files = new HgtFiles(null, new DownloadManager(null));

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
