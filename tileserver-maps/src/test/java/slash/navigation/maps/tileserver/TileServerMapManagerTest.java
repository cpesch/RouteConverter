package slash.navigation.maps.tileserver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.navigation.maps.tileserver.TileServerMapManager.extractCopyrightHref;

public class TileServerMapManagerTest {
    @Test
    public void testExtractHref() {
        assertEquals("http://www.wanderreitkarte.de/", extractCopyrightHref("Map data &copy; <a href=\"http://www.wanderreitkarte.de/\" target=\"_blank\">Wanderreitkarte</a>"));
        assertEquals("http://www.wanderreitkarte.de/", extractCopyrightHref("href=\"http://www.wanderreitkarte.de/\""));
        assertNull(extractCopyrightHref("http://www.wanderreitkarte.de/"));
    }
}
