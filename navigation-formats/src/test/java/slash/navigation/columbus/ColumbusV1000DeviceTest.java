package slash.navigation.columbus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ColumbusV1000DeviceTest {
    private boolean useLocalTimeZone;
    private String timeZone;

    @Before
    public void setUp() {
        useLocalTimeZone = ColumbusV1000Device.getUseLocalTimeZone();
        timeZone = ColumbusV1000Device.getTimeZone();
    }

    @After
    public void tearDown() {
        ColumbusV1000Device.setUseLocalTimeZone(useLocalTimeZone);
        ColumbusV1000Device.setTimeZone(timeZone);
    }

    @Test
    public void testUseLocalTimeZoneRoundtrip() {
        ColumbusV1000Device.setUseLocalTimeZone(false);
        assertFalse(ColumbusV1000Device.getUseLocalTimeZone());
        ColumbusV1000Device.setUseLocalTimeZone(true);
        assertTrue(ColumbusV1000Device.getUseLocalTimeZone());
    }

    @Test
    public void testTimeZoneRoundtrip() {
        ColumbusV1000Device.setTimeZone("Europe/Berlin");
        assertEquals("Europe/Berlin", ColumbusV1000Device.getTimeZone());
    }
}
