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

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Reads little-endian binary data from a byte array. Reading past the end throws an {@link EOFException}.
 */
public class LittleEndianInput {
    private final byte[] data;
    private int position;

    public LittleEndianInput(byte[] data) {
        this.data = data;
    }

    public int position() {
        return position;
    }

    public void seek(int position) throws EOFException {
        if (position < 0 || position > data.length)
            throw new EOFException("Cannot seek to " + position + " in " + data.length + " bytes");
        this.position = position;
    }

    public void skip(int count) throws EOFException {
        require(count);
        position += count;
    }

    public int remaining() {
        return data.length - position;
    }

    private void require(int count) throws EOFException {
        if (count < 0 || count > remaining())
            throw new EOFException("Cannot read " + count + " bytes at " + position + " of " + data.length);
    }

    private ByteBuffer buffer(int count) throws EOFException {
        require(count);
        ByteBuffer buffer = ByteBuffer.wrap(data, position, count).order(ByteOrder.LITTLE_ENDIAN);
        position += count;
        return buffer;
    }

    public int readUInt8() throws EOFException {
        return buffer(1).get() & 0xFF;
    }

    public int readInt16() throws EOFException {
        return buffer(2).getShort();
    }

    public int readUInt16() throws EOFException {
        return buffer(2).getShort() & 0xFFFF;
    }

    public int readInt32() throws EOFException {
        return buffer(4).getInt();
    }

    public long readUInt32() throws EOFException {
        return buffer(4).getInt() & 0xFFFFFFFFL;
    }

    public float readFloat() throws EOFException {
        return buffer(4).getFloat();
    }

    public double readDouble() throws EOFException {
        return buffer(8).getDouble();
    }

    public byte[] readBytes(int count) throws EOFException {
        require(count);
        byte[] result = Arrays.copyOfRange(data, position, position + count);
        position += count;
        return result;
    }

    /**
     * Reads a string preceded by a 1-byte length.
     */
    public String readPascalString(Charset charset) throws EOFException {
        int length = readUInt8();
        return new String(readBytes(length), charset);
    }

    /**
     * Reads a string up to the next NUL byte and consumes the NUL.
     */
    public String readCString(Charset charset) throws EOFException {
        int end = position;
        while (end < data.length && data[end] != 0)
            end++;
        if (end >= data.length)
            throw new EOFException("No terminating NUL after position " + position);
        String result = new String(data, position, end - position, charset);
        position = end + 1;
        return result;
    }

    /**
     * Reads a string of exactly the given number of bytes without trimming.
     */
    public String readFixedString(int length, Charset charset) throws EOFException {
        return new String(readBytes(length), charset);
    }
}
