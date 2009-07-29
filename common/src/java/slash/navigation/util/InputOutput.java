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

package slash.navigation.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Reads from input and writes to output. If start is called this
 * class' instances will read all available data from the InputStream
 * and write it to the OutputStream. Think of it as a U-pipe with a
 * buffer.
 *
 * @author Christian Pesch
 */

public class InputOutput {
    private static final int CHUNK_SIZE = (4 * 1024);

    private int chunkSize = CHUNK_SIZE;
    private final InputStream input;
    private final OutputStream output;

    public InputOutput(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * When started this read all available data from the
     * InputStream and write it to the OutputStream.
     * @throws IOException is forwared
     */
    public void start() throws IOException {
        byte[] chunk = new byte[chunkSize];

        while (true) {
            int read = input.read(chunk);
            if (read == -1) {
                // no more data available
                break;
            }

            output.write(chunk, 0, read);
        }

        input.close();
        output.close();
    }

    public void close() throws IOException {
        input.close();
        output.close();
    }

    public static byte[] readBytes(URL url) throws IOException {
        return readBytes(url.openStream());
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputOutput pipe = new InputOutput(in, out);
        pipe.start();
        pipe.close();
        return out.toByteArray();
    }
}
