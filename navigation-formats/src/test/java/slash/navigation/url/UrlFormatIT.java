package slash.navigation.url;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx11Format;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class UrlFormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    @Test
    public void readRouteCatalogUrl() throws IOException {
        ParserResult result = parser.read("https://static.routeconverter.com/routes/2ce409b0-06b3-424e-9556-5e0765714f6b");
        assertNotNull(result);
        assertEquals(1, result.getAllRoutes().size());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
    }

    @Test
    public void readGoogleMapsUrl() throws IOException {
        ParserResult result = parser.read("http://maps.google.de/maps?f=d&saddr=Hamburg%2FUhlenhorst&daddr=Hauptstra%C3%9Fe%2FL160+to:53.588429,10.419159+to:Breitenfelde%2FNeuenlande&hl=de&geocode=%3BFVy1MQMdDoudAA%3B%3B&mra=dpe&mrcr=0&mrsp=2&sz=11&via=1,2&sll=53.582575,10.30528&sspn=0.234798,0.715485&ie=UTF8&z=11");
        assertNotNull(result);
        assertEquals(1, result.getAllRoutes().size());
        assertEquals(4, result.getTheRoute().getPositionCount());
        assertEquals(GoogleMapsUrlFormat.class, result.getFormat().getClass());
    }

    @Test
    public void readURLReference() throws IOException {
        ParserResult result = parser.read(new File(TEST_PATH + "from-gpx.url"));
        assertNotNull(result);
        assertEquals(4, result.getAllRoutes().size());
        assertEquals(3, result.getTheRoute().getPositionCount());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
    }
}
