package slash.common.type;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static slash.common.type.HexadecimalNumber.decodeBytes;
import static slash.common.type.HexadecimalNumber.encodeByte;
import static slash.common.type.HexadecimalNumber.encodeBytes;
import static slash.common.type.HexadecimalNumber.encodeColor;

public class HexadecimalNumberTest {
    @Test
    public void testEncodeBytes() {
        assertEquals("01", encodeByte((byte) 1));
        assertEquals("010203", encodeBytes(new byte[]{1, 2, 3}));
    }

    @Test
    public void testDecodeBytes() {
        assertArrayEquals(new byte[]{1, 2, 3}, decodeBytes("010203"));
    }

    @Test
    public void testEncodeColor() {
        assertEquals("6CB1F3", encodeColor(new Color(7123443)));
        assertEquals("0033FF", encodeColor(new Color(13311)));
    }
}


