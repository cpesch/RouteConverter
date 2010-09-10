package slash.navigation.base;

import org.junit.Test;
import slash.navigation.babel.AlanTrackLogFormat;
import slash.navigation.babel.GarminMapSource6Format;
import slash.navigation.babel.TomTomPoiFormat;
import slash.navigation.simple.BrokenHaicomLoggerFormat;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.ColumbusV900StandardFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Route66Format;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class NavigationFormatsTest {

    @Test
    public void testGetReadFormatsSortedByExtension() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsSortedByExtension(".ov2");
        assertEquals(TomTomPoiFormat.class, formats.get(0).getClass());
        assertEquals(AlanTrackLogFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtension2() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsSortedByExtension(".gdb");
        assertEquals(GarminMapSource6Format.class, formats.get(0).getClass());
        assertEquals(AlanTrackLogFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionMultipleResults() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsSortedByExtension(".csv");
        assertEquals(HaicomLoggerFormat.class, formats.get(0).getClass());
        assertEquals(Route66Format.class, formats.get(1).getClass());
        assertEquals(ColumbusV900ProfessionalFormat.class, formats.get(2).getClass());
        assertEquals(ColumbusV900StandardFormat.class, formats.get(3).getClass());
        assertEquals(BrokenHaicomLoggerFormat.class, formats.get(4).getClass());
        assertEquals(AlanTrackLogFormat.class, formats.get(5).getClass());
    }
}
