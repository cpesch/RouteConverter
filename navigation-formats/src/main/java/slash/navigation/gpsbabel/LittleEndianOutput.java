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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Writes little-endian binary data into a growing byte array.
 */
public class LittleEndianOutput {
    private static final int MAX_PASCAL_LENGTH = 255;

    private byte[] data = new byte[64];
    private int size;

    public int size() {
        return size;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(data, size);
    }

    private void ensure(int count) {
        if (size + count > data.length)
            data = Arrays.copyOf(data, Math.max(data.length * 2, size + count));
    }

    private ByteBuffer buffer(int count) {
        ensure(count);
        ByteBuffer buffer = ByteBuffer.wrap(data, size, count).order(ByteOrder.LITTLE_ENDIAN);
        size += count;
        return buffer;
    }

    public void writeUInt8(int value) {
        buffer(1).put((byte) value);
    }

    public void writeInt16(int value) {
        buffer(2).putShort((short) value);
    }

    public void writeUInt16(int value) {
        buffer(2).putShort((short) value);
    }

    public void writeInt32(int value) {
        buffer(4).putInt(value);
    }

    public void writeUInt32(long value) {
        buffer(4).putInt((int) value);
    }

    public void writeFloat(float value) {
        buffer(4).putFloat(value);
    }

    public void writeDouble(double value) {
        buffer(8).putDouble(value);
    }

    public void writeBytes(byte[] bytes) {
        buffer(bytes.length).put(bytes);
    }

    /**
     * Writes a 1-byte length followed by the encoded string, cut to 255 bytes.
     */
    public void writePascalString(String string, Charset charset) {
        byte[] bytes = string.getBytes(charset);
        int length = Math.min(bytes.length, MAX_PASCAL_LENGTH);
        writeUInt8(length);
        buffer(length).put(bytes, 0, length);
    }

    /**
     * Writes the encoded string followed by a NUL byte.
     */
    public void writeCString(String string, Charset charset) {
        writeBytes(string.getBytes(charset));
        writeUInt8(0);
    }

    /**
     * Writes the encoded string cut or padded with NUL bytes to exactly the given number of bytes.
     */
    public void writeFixedString(String string, int length, Charset charset) {
        byte[] bytes = string.getBytes(charset);
        ensure(length);
        int copy = Math.min(bytes.length, length);
        System.arraycopy(bytes, 0, data, size, copy);
        Arrays.fill(data, size + copy, size + length, (byte) 0);
        size += length;
    }

    /**
     * Overwrites the 32-bit value at the given offset, e.g. a length known only after writing the content.
     */
    public void patchInt32(int offset, int value) {
        if (offset < 0 || offset + 4 > size)
            throw new IndexOutOfBoundsException("Cannot patch 4 bytes at " + offset + " of " + size);
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
    }
}
