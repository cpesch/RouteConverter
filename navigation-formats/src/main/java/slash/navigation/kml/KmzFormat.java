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

package slash.navigation.kml;

import slash.common.io.NotClosingUnderlyingInputStream;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * The base of all compressed Google Earth formats.
 *
 * @author Christian Pesch
 */

public abstract class KmzFormat extends BaseKmlFormat {
    private static final Logger log = Logger.getLogger(KmzFormat.class.getName());
    private final KmlFormat delegate;

    protected KmzFormat(KmlFormat delegate) {
        this.delegate = delegate;
    }

    public String getExtension() {
        return ".kmz";
    }

    public boolean isSupportsMultipleRoutes() {
        return delegate.isSupportsMultipleRoutes();
    }

    public boolean isWritingRouteCharacteristics() {
        return delegate.isWritingRouteCharacteristics();
    }

    public <P extends NavigationPosition> KmlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return delegate.createRoute(characteristics, name, positions);
    }

    /**
     * Upper bound for the bytes that may be inflated from one KMZ, all entries together. A KMZ is a
     * zip, and a few kilobytes can inflate to gigabytes (zip bomb). Override with
     * {@code -Drc.kmz.max.uncompressed.bytes=...}, e.g. lower for a server that converts uploads.
     */
    static final String MAX_UNCOMPRESSED_BYTES_PROPERTY = "rc.kmz.max.uncompressed.bytes";
    static final long DEFAULT_MAX_UNCOMPRESSED_BYTES = 256L * 1024 * 1024;
    static final int MAX_ENTRIES = 1000;
    private static final Pattern RESOURCE_ENTRY = Pattern.compile(".*\\.(png|jpe?g|gif|bmp|tiff?|ico|svg|dae|mp3|wav|ogg|mp4)$", CASE_INSENSITIVE);

    static long getMaxUncompressedBytes() {
        return Long.getLong(MAX_UNCOMPRESSED_BYTES_PROPERTY, DEFAULT_MAX_UNCOMPRESSED_BYTES);
    }

    private static class BudgetInputStream extends FilterInputStream {
        private final long limit;
        private long consumed;
        private boolean exceeded;

        BudgetInputStream(InputStream in, long limit) {
            super(in);
            this.limit = limit;
        }

        private void count(long bytes) throws IOException {
            if (bytes <= 0)
                return;
            consumed += bytes;
            if (consumed > limit) {
                exceeded = true;
                throw new IOException(format("KMZ inflates to more than %d bytes; refusing to read it", limit));
            }
        }

        public int read() throws IOException {
            int result = super.read();
            if (result != -1)
                count(1);
            return result;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            count(result);
            return result;
        }

        public long skip(long n) throws IOException {
            // route skipping through read() so skipped bytes count as well
            byte[] buffer = new byte[(int) Math.min(n, 8192)];
            int read = read(buffer, 0, buffer.length);
            return Math.max(read, 0);
        }
    }

    public void read(InputStream source, ParserContext<KmlRoute> context) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(source)) {
            BudgetInputStream budget = new BudgetInputStream(zip, getMaxUncompressedBytes());
            ZipEntry entry;
            int entries = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES)
                    throw new IOException(format("KMZ contains more than %d entries; refusing to read it", MAX_ENTRIES));
                // icons and models are never KML; do not feed them to the XML parser
                if(entry.isDirectory() || RESOURCE_ENTRY.matcher(entry.getName()).matches())
                    continue;

                try {
                    delegate.read(new NotClosingUnderlyingInputStream(budget), context);
                }
                catch(Exception e) {
                    if (budget.exceeded)
                        throw new IOException(format("KMZ inflates to more than %d bytes; refusing to read it", budget.limit), e);
                    log.info(format("Error reading %s with %s: %s, %s", entry, delegate, e.getClass(), e));
                }
                // inflate (and count) what the delegate left unread instead of letting closeEntry() skip it unmetered
                budget.transferTo(OutputStream.nullOutputStream());
                zip.closeEntry();
            }
        }
        if(context.getFormats().isEmpty())
            throw new IOException(format("Cannot find %s format in %s", getName(), context.getFile()));
    }

    private void writeIntermediate(OutputStream target, byte[] bytes) throws IOException {
        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(bytes);

        try(ZipOutputStream outputStream = new ZipOutputStream(target)) {
            ZipEntry entry = new ZipEntry("doc.kml");
            entry.setSize(bytes.length);
            entry.setCrc(crc.getValue());
            outputStream.putNextEntry(entry);
            outputStream.write(bytes, 0, bytes.length);
            outputStream.finish();
        }
    }

    public void write(KmlRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        delegate.write(route, baos, startIndex, endIndex);
        writeIntermediate(target, baos.toByteArray());
    }

    public void write(List<KmlRoute> routes, OutputStream target) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        delegate.write(routes, baos);
        writeIntermediate(target, baos.toByteArray());
    }
}
