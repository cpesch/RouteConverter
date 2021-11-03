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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.lang.String.format;

/**
 * The base of all compressed Google Earth formats.
 *
 * @author Christian Pesch
 */

public abstract class KmzFormat extends BaseKmlFormat {
    private static final Logger log = Logger.getLogger(KmzFormat.class.getName());
    private KmlFormat delegate;

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

    public void read(InputStream source, ParserContext<KmlRoute> context) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if(entry.isDirectory())
                    continue;

                try {
                    delegate.read(new NotClosingUnderlyingInputStream(zip), context);
                }
                catch(Exception e) {
                    log.info(format("Error reading %s with %s: %s, %s", entry, delegate, e.getClass(), e));
                }
                zip.closeEntry();
            }
        }
        if(context.getFormats().size() == 0)
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
