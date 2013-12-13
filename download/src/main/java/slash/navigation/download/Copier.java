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
package slash.navigation.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Copies an {@link InputStream} to an {@link OutputStream} and notifies about i.
 *
 * @author Christian Pesch
 */
public class Copier {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private Download download;
    private DownloadTableModel model;

    Copier(Download download, DownloadTableModel model) {
        this.download = download;
        this.model = model;
    }

    private void updateDownloadedBytes(Download download, long downloadedBytes) {
        download.setProcessedBytes(downloadedBytes);
        model.updateDownload(download);
    }

    public long copyAndClose(InputStream input, OutputStream output, long startByte) throws IOException {
        try {
            return copy(input, output, startByte);
        } finally {
            try {
                closeQuietly(input);
            } finally {
                closeQuietly(output);
            }
        }
    }

    public long copy(InputStream input, OutputStream output, long startByte) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long totalBytes = startByte;
        int read;

        while (-1 != (read = input.read(buffer))) {
            output.write(buffer, 0, read);
            totalBytes += read;
            updateDownloadedBytes(download, totalBytes);
        }
        return totalBytes;
    }
}
