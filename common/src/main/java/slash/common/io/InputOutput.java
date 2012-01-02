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

package slash.common.io;

import java.io.*;
import java.net.URL;

/**
 * As a pipe reads from input and writes to output.
 *
 * @author Christian Pesch
 */

public class InputOutput {
    private static final int CHUNK_SIZE = (4 * 1024);

    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        int count = 0;
        int read;
        try {
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                count += read;
            }
        } finally {
            input.close();
            output.close();
        }
        return count;
    }

    public static int copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[CHUNK_SIZE];
        int count = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            count += read;
        }
        input.close();
        output.close();
        return count;
    }

    public static byte[] readBytes(URL url) throws IOException {
        return readBytes(url.openStream());
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }
}
