package slash.navigation.columbus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static jakarta.xml.bind.DatatypeConverter.parseHexBinary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.calendar;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.Waypoint;

public class ColumbusGpsBinaryFormatTest {
    private final ColumbusGpsBinaryFormat format = new ColumbusGpsBinaryFormat();
    private boolean useLocalTimeZone;

    @Before
    public void setUp() {
        useLocalTimeZone = ColumbusV1000Device.getUseLocalTimeZone();
        ColumbusV1000Device.setUseLocalTimeZone(false);
    }

    @After
    public void tearDown() {
        ColumbusV1000Device.setUseLocalTimeZone(useLocalTimeZone);
    }

    @Test
    public void testHasBitSet() {
        assertFalse(format.hasBitSet((byte)0, 3));
        assertTrue(format.hasBitSet((byte)0x0C, 3));
        assertTrue(format.hasBitSet((byte)0x0C, 2));
    }

    @Test
    public void testParseCoordinates() {
        ByteBuffer buffer1 = ByteBuffer.wrap(parseHexBinary("00989680"));
        assertDoubleEquals(10.0, format.parseCoordinate(buffer1.getInt(), false));
        ByteBuffer buffer2 = ByteBuffer.wrap(parseHexBinary("00989680"));
        assertDoubleEquals(-10.0, format.parseCoordinate(buffer2.getInt(), true));
    }

    @Test
    public void testParseTag() {
        assertEquals(Waypoint, format.parseTag((byte) 0x00));         // neither bit 0 nor bit 1
        assertEquals(PointOfInterestC, format.parseTag((byte) 0x01)); // bit 0 set, bit 1 clear
        assertEquals(Waypoint, format.parseTag((byte) 0x02));         // bit 1 set
        assertEquals(Waypoint, format.parseTag((byte) 0x03));         // bit 0 and bit 1 set
    }

    @Test
    public void testParseTime() {
        // packed fields: year=2 (2016+2), month=11 (stored, => November), day=29, hour=12, minute=0, second=2
        int packed = (2 << 26) | (11 << 22) | (29 << 17) | (12 << 12) | (0 << 6) | 2;
        assertEquals(calendar(2018, 11, 29, 12, 0, 2), format.parseTime(packed));
    }
}
