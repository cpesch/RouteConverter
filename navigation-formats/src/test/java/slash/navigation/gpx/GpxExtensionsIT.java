package slash.navigation.gpx;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.readGpxFile;

public class GpxExtensionsIT {

    private GpxPosition readPosition(String fileName) throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx11Format(), fileName);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
        return route.getPosition(0);
    }

    private void checkPositionBasics(GpxPosition position) {
        assertEquals("one", position.getDescription());
        assertDoubleEquals(50.8758450, position.getLatitude());
        assertDoubleEquals(4.6710150, position.getLongitude());
        assertDoubleEquals(60.0, position.getElevation());
        assertEquals(calendar(2014, 6, 15, 15, 45, 39), position.getTime());
    }

    @Test
    public void testReadGarminGpxExtensionv3() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "garmin-gpx-extension-v3.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(25.0, position.getTemperature());
    }

    @Test
    public void testReadGarminTrackPointExtensionv1() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "garmin-track-point-extension-v1.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(25.0, position.getTemperature());
    }

    @Test
    public void testReadGarminTrackPointExtensionv2() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "garmin-track-point-extension-v2.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(25.0, position.getTemperature());
        assertDoubleEquals(43.2, position.getSpeed());
        assertDoubleEquals(98.0, position.getHeading());
    }

    @Test
    public void testReadTrekbuddyExtension1() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "trekbuddy-extension-1.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(15.12, position.getSpeed());
        assertDoubleEquals(124.5, position.getHeading());
    }

    @Test
    public void testReadTrekbuddyExtension2() throws Exception {
        GpxPosition position = readPosition(TEST_PATH + "trekbuddy-extension-2.gpx");
        checkPositionBasics(position);
        assertDoubleEquals(15.12, position.getSpeed());
        assertDoubleEquals(124.5, position.getHeading());
    }
}
