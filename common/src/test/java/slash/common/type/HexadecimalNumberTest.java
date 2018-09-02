package slash.common.type;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static slash.common.type.HexadecimalNumber.decodeBytes;
import static slash.common.type.HexadecimalNumber.decodeInt;
import static slash.common.type.HexadecimalNumber.encodeByte;
import static slash.common.type.HexadecimalNumber.encodeBytes;
import static slash.common.type.HexadecimalNumber.encodeInt;

public class HexadecimalNumberTest {
    @Test
    public void testEncodeBytes() {
        assertEquals("01", encodeByte((byte) 1));
        assertEquals("010203", encodeBytes(new byte[]{1, 2, 3}));
    }

    @Test
    public void testDecodeBytes() {
        assertArrayEquals(new byte[]{1, 2, 3}, decodeBytes("010203"));
        assertArrayEquals(new byte[]{2, 3, 4}, decodeBytes("020304"));
    }

    @Test
    public void testEncodeInt() {
        assertEquals("2030405", encodeInt(2 * 256 * 256 * 256 + 3 * 256 * 256 + 4 * 256 + 5));
    }

    @Test
    public void testDecodeInt() {
        assertEquals(2 * 256 * 256 + 3 * 256 + 4, decodeInt("020304"));
    }
}


