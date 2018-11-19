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
package slash.navigation.download.actions;

import java.io.*;

import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.InputOutput.DEFAULT_BUFFER_SIZE;
import static slash.common.io.InputOutput.closeQuietly;

/**
 * Copies an {@link InputStream} to an {@link OutputStream} and notifies about it.
 *
 * @author Christian Pesch
 */
public class Copier {
    private final CopierListener listener;

    public Copier(CopierListener listener) {
        this.listener = listener;
    }

    public long copyAndClose(File from, File to) throws IOException {
        ensureDirectory(to.getParent());
        try(InputStream inputStream = new FileInputStream(from); OutputStream outputStream = new FileOutputStream(to)) {
            return copyAndClose(inputStream, outputStream, 0, from.length());
        }
    }

    public long copyAndClose(InputStream input, OutputStream output, long startByte, Long expectingBytes) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(input);
        BufferedOutputStream bos = new BufferedOutputStream(output);
        try {
            return copy(bis, bos, startByte, expectingBytes);
        } finally {
            try {
                closeQuietly(bis);
            } finally {
                closeQuietly(bos);
            }
        }
    }

    public long copy(InputStream input, OutputStream output, long startByte, Long bytes) throws IOException {
        Long expectingBytes = bytes != null && bytes > 10 ? bytes : null;
        if (expectingBytes == null)
            expectingBytes = input.available() > 10 ? (long) input.available() : null;
        if (expectingBytes != null)
            listener.expectingBytes(expectingBytes);

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long totalBytes = startByte;
        int read;

        while (-1 != (read = input.read(buffer))) {
            output.write(buffer, 0, read);
            totalBytes += read;
            listener.processedBytes(totalBytes);
        }
        return totalBytes;
    }
}
