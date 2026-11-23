package slash.navigation.columbus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.common.prefs.InMemoryPreferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColumbusV1000DeviceTest {
    @BeforeEach
    public void setUp() {
        ColumbusV1000Device.setPreferences(new InMemoryPreferences());
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
