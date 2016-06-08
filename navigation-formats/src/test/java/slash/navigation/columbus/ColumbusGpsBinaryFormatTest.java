package slash.navigation.columbus;

import org.junit.Test;

import java.nio.ByteBuffer;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class ColumbusGpsBinaryFormatTest {
    private ColumbusGpsBinaryFormat format = new ColumbusGpsBinaryFormat();

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
}
