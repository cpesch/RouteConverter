package slash.navigation.base;

import org.junit.Test;
import slash.navigation.babel.GarminMapSource6Format;
import slash.navigation.babel.TomTomPoiFormat;
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.simple.*;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class NavigationFormatsTest {

    @Test
    public void testGetReadFormatsSortedByExtension() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsPreferredByExtension(".ov2");
        assertEquals(TomTomPoiFormat.class, formats.get(0).getClass());
        assertEquals(NmeaFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionIsCaseSensitive() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsPreferredByExtension(".OV2");
        assertEquals(NmeaFormat.class, formats.get(0).getClass());
        assertEquals(MTP0809Format.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtension2() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsPreferredByExtension(".gdb");
        assertEquals(GarminMapSource6Format.class, formats.get(0).getClass());
        assertEquals(NmeaFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionMultipleResults() {
        List<NavigationFormat> formats = NavigationFormats.getReadFormatsPreferredByExtension(".csv");
        assertEquals(HaicomLoggerFormat.class, formats.get(0).getClass());
        assertEquals(Route66Format.class, formats.get(1).getClass());
        assertEquals(ColumbusV900ProfessionalFormat.class, formats.get(2).getClass());
        assertEquals(ColumbusV900StandardFormat.class, formats.get(3).getClass());
        assertEquals(QstarzQ1000Format.class, formats.get(4).getClass());
        assertEquals(Iblue747Format.class, formats.get(5).getClass());
        assertEquals(BrokenHaicomLoggerFormat.class, formats.get(6).getClass());
        assertEquals(NmeaFormat.class, formats.get(7).getClass());
    }
}
