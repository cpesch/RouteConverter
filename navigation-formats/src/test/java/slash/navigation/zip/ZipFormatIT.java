package slash.navigation.zip;

import org.junit.Test;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.kml.Kml22Format;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationFormats.getReadFormatsPreferredByExtension;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class ZipFormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser();

    @Test
    public void readGPX11Archive() throws IOException {
        ParserResult result = parser.read(new File(TEST_PATH + "from-gpx11.zip"));
        assertNotNull(result);
        assertEquals(4, result.getAllRoutes().size());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
    }

    @Test
    public void readGPX11AndKML22Archive() throws IOException {
        ParserResult result = parser.read(new File(TEST_PATH + "from-gpx-kml.zip"), getReadFormatsPreferredByExtension(".zip"));
        assertNotNull(result);
        assertEquals(7, result.getAllRoutes().size());
        assertEquals(Kml22Format.class, result.getFormat().getClass());
    }

    @Test
    public void readArchiveInArchive() throws IOException {
        ParserResult result = parser.read(new File(TEST_PATH + "from-zip.zip"));
        assertNotNull(result);
        assertEquals(4, result.getAllRoutes().size());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
    }
}
