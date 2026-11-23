/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.gpsbabel;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LittleEndianTest {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Test
    public void testReadersAndWriters() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writeUInt8(255);
        out.writeInt16(-2);
        out.writeUInt16(65535);
        out.writeInt32(-123456789);
        out.writeUInt32(4294967295L);
        out.writeFloat(-1.5f);
        out.writeDouble(-2.25);
        out.writeBytes(new byte[]{1, 2, 3});
        assertEquals(1 + 2 + 2 + 4 + 4 + 4 + 8 + 3, out.size());

        LittleEndianInput in = new LittleEndianInput(out.toByteArray());
        assertEquals(255, in.readUInt8());
        assertEquals(-2, in.readInt16());
        assertEquals(65535, in.readUInt16());
        assertEquals(-123456789, in.readInt32());
        assertEquals(4294967295L, in.readUInt32());
        assertEquals(-1.5f, in.readFloat(), 0.0f);
        assertEquals(-2.25, in.readDouble(), 0.0);
        assertArrayEquals(new byte[]{1, 2, 3}, in.readBytes(3));
        assertEquals(0, in.remaining());
    }

    @Test
    public void testLittleEndianByteOrder() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writeInt32(0x01020304);
        out.writeUInt16(0xFFFE);
        assertArrayEquals(new byte[]{4, 3, 2, 1, (byte) 0xFE, (byte) 0xFF}, out.toByteArray());
        LittleEndianInput in = new LittleEndianInput(new byte[]{(byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF});
        assertEquals(-2, in.readInt16());
        assertEquals(65534, in.readUInt16());
    }

    @Test
    public void testNegativeAndSignedValues() throws Exception {
        LittleEndianInput in = new LittleEndianInput(new byte[]{(byte) 0x80, (byte) 0x80, 0});
        assertEquals(128, in.readUInt8());
        assertEquals(128, in.readUInt16());
        assertEquals(-32768, new LittleEndianInput(new byte[]{0, (byte) 0x80}).readInt16());
        assertEquals(Integer.MIN_VALUE, new LittleEndianInput(new byte[]{0, 0, 0, (byte) 0x80}).readInt32());
        assertEquals(2147483648L, new LittleEndianInput(new byte[]{0, 0, 0, (byte) 0x80}).readUInt32());
    }

    @Test
    public void testPositionSeekSkip() throws Exception {
        LittleEndianInput in = new LittleEndianInput(new byte[]{1, 2, 3, 4, 5});
        assertEquals(0, in.position());
        in.skip(2);
        assertEquals(2, in.position());
        assertEquals(3, in.remaining());
        assertEquals(3, in.readUInt8());
        in.seek(0);
        assertEquals(1, in.readUInt8());
        in.seek(5);
        assertEquals(0, in.remaining());
    }

    @Test
    public void testEofUInt8() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[0]).readUInt8());
    }

    @Test
    public void testEofInt16() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[1]).readInt16());
    }

    @Test
    public void testEofUInt16() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[1]).readUInt16());
    }

    @Test
    public void testEofInt32() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[3]).readInt32());
    }

    @Test
    public void testEofUInt32() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[3]).readUInt32());
    }

    @Test
    public void testEofFloat() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[3]).readFloat());
    }

    @Test
    public void testEofDouble() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[7]).readDouble());
    }

    @Test
    public void testEofBytes() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[2]).readBytes(3));
    }

    @Test
    public void testEofSkip() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[2]).skip(3));
    }

    @Test
    public void testEofSeek() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[2]).seek(3));
    }

    @Test
    public void testEofFixedString() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[2]).readFixedString(3, UTF_8));
    }

    @Test
    public void testEofPascalStringBody() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[]{5, 'a'}).readPascalString(UTF_8));
    }

    @Test
    public void testEofCStringWithoutNul() throws Exception {
        assertThrows(EOFException.class, () ->
            new LittleEndianInput(new byte[]{'a', 'b'}).readCString(UTF_8));
    }

    @Test
    public void testPascalString255Bytes() throws Exception {
        String s = "x".repeat(255);
        LittleEndianOutput out = new LittleEndianOutput();
        out.writePascalString(s, UTF_8);
        assertEquals(256, out.size());
        assertEquals(255, out.toByteArray()[0] & 0xFF);
        assertEquals(s, new LittleEndianInput(out.toByteArray()).readPascalString(UTF_8));
    }

    @Test
    public void testPascalStringCutAt256() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writePascalString("y".repeat(256), UTF_8);
        assertEquals(256, out.size());
        LittleEndianInput in = new LittleEndianInput(out.toByteArray());
        assertEquals("y".repeat(255), in.readPascalString(UTF_8));
        assertEquals(0, in.remaining());
    }

    @Test
    public void testEmptyPascalString() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writePascalString("", UTF_8);
        assertArrayEquals(new byte[]{0}, out.toByteArray());
        assertEquals("", new LittleEndianInput(out.toByteArray()).readPascalString(UTF_8));
    }

    @Test
    public void testCStringWithUmlautsWindows1252() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writeCString("Müller-Straße", WINDOWS_1252);
        out.writeUInt8(7);
        byte[] bytes = out.toByteArray();
        assertEquals(13 + 1 + 1, bytes.length);
        assertEquals((byte) 0xFC, bytes[1]);
        LittleEndianInput in = new LittleEndianInput(bytes);
        assertEquals("Müller-Straße", in.readCString(WINDOWS_1252));
        assertEquals(7, in.readUInt8());
    }

    @Test
    public void testCStringWithUmlautsUtf8() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writeCString("Äpfel öfter über", UTF_8);
        out.writeCString("", UTF_8);
        LittleEndianInput in = new LittleEndianInput(out.toByteArray());
        assertEquals("Äpfel öfter über", in.readCString(UTF_8));
        assertEquals("", in.readCString(UTF_8));
        assertEquals(0, in.remaining());
    }

    @Test
    public void testFixedStringIsNotTrimmed() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writeFixedString("ab", 5, UTF_8);
        out.writeFixedString("abcdefg", 3, UTF_8);
        assertArrayEquals(new byte[]{'a', 'b', 0, 0, 0, 'a', 'b', 'c'}, out.toByteArray());
        LittleEndianInput in = new LittleEndianInput(out.toByteArray());
        assertEquals("ab\0\0\0", in.readFixedString(5, UTF_8));
        assertEquals("abc", in.readFixedString(3, UTF_8));
    }

    @Test
    public void testPatchInt32() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        out.writeUInt8(9);
        int offset = out.size();
        out.writeInt32(0);
        out.writeBytes(new byte[100]);
        out.patchInt32(offset, -2);
        LittleEndianInput in = new LittleEndianInput(out.toByteArray());
        assertEquals(9, in.readUInt8());
        assertEquals(-2, in.readInt32());
        assertEquals(100, in.remaining());
    }

    @Test
    public void testGrowth() throws Exception {
        LittleEndianOutput out = new LittleEndianOutput();
        for (int i = 0; i < 1000; i++)
            out.writeInt32(i);
        LittleEndianInput in = new LittleEndianInput(out.toByteArray());
        for (int i = 0; i < 1000; i++)
            assertEquals(i, in.readInt32());
    }
}
