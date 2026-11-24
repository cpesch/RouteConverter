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

package slash.navigation.zip;

import com.fasterxml.jackson.databind.JsonNode;
import slash.common.io.Files;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Reads routes from ZIP Archive (.zip) files.
 *
 * @author Christian Pesch
 */

public class ZipFormat extends BaseNavigationFormat<BaseRoute<?, ?>> {
    private static final Logger log = Logger.getLogger(ZipFormat.class.getName());
    // readNBytes() needs about twice the entry size while it collects the bytes
    static final int MAXIMUM_ENTRY_SIZE = 64 * 1024 * 1024;
    // tests lower it to check the cap without allocating 64 MB
    static volatile int maximumEntrySize = MAXIMUM_ENTRY_SIZE;
    private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
    static {
        System.setProperty("sun.zip.encoding", "default");
    }

    public String getName() {
        return "ZIP Archive (" + getExtension() + ")";
    }

    public String getExtension() {
        return ".zip";
    }

    public int getMaximumPositionCount() {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
       return false;
    }

    public boolean isWritingRouteCharacteristics() {
        throw new UnsupportedOperationException();
    }

    public <P extends NavigationPosition> BaseRoute<?, ?> createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, ParserContext<BaseRoute<?, ?>> context) throws IOException {
        int level = depth.get();
        depth.set(level + 1);
        try {
            // a ZIP file of its own is read through its central directory: entry sizes are known
            // even for streamed archives, and a corrupt entry does not end reading the others;
            // nested archives and other sources are read as a stream
            File file = context.getFile();
            PhotoSidecars photoSidecars = new PhotoSidecars();
            if (level == 0 && file != null && file.isFile() && getExtension().equals(Files.getExtension(file))) {
                if (!readFile(file, photoSidecars, context))
                    readStream(source, photoSidecars, context);
            } else
                readStream(source, photoSidecars, context);
            photoSidecars.removePhotosWithoutCoordinates(context);
        } finally {
            depth.set(level);
        }
    }

    // @return false if the file has no readable central directory, e.g. a partial download
    private boolean readFile(File file, PhotoSidecars photoSidecars, ParserContext<BaseRoute<?, ?>> context) throws IOException {
        ZipFile opened;
        try {
            opened = new ZipFile(file);
        } catch (ZipException e) {
            log.info("Reading " + file + " as a stream, it has no readable central directory: " + e);
            return false;
        }
        try (ZipFile zipFile = opened) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory())
                    continue;
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    readEntry(entry.getName(), inputStream, photoSidecars, context);
                } catch (IOException e) {
                    log.warning("Cannot read zip entry " + entry.getName() + " from " + file + ": " + e);
                }
            }
        }
        return true;
    }

    private void readStream(InputStream source, PhotoSidecars photoSidecars, ParserContext<BaseRoute<?, ?>> context) {
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                readEntry(entry.getName(), zip, photoSidecars, context);
                zip.closeEntry();
            }
        } catch (IOException e) {
            // a stream cannot skip a corrupt entry: the entries after it are lost
            log.warning("Cannot read further zip entries from " + source + ": " + e);
        }
    }

    // reads the whole entry so that every format probe can reset() to its start: streamed archives
    // like Google Takeout carry the size in a data descriptor after the data, so ZipInputStream
    // reports -1 and a mark() derived from it broke reset() after 8 KB
    private void readEntry(String entryName, InputStream inputStream, PhotoSidecars photoSidecars,
                           ParserContext<BaseRoute<?, ?>> context) throws IOException {
        byte[] bytes;
        try {
            bytes = inputStream.readNBytes(maximumEntrySize + 1);
        } catch (OutOfMemoryError e) {
            log.warning("Skipping zip entry " + entryName + ": not enough memory to read it");
            return;
        }
        if (bytes.length > maximumEntrySize) {
            log.warning("Skipping zip entry " + entryName + " larger than " + maximumEntrySize + " bytes");
            return;
        }

        // photo sidecars are recognized by their content and never probed as navigation files
        JsonNode sidecar = PhotoSidecars.parseSidecar(entryName, bytes);
        if (sidecar != null) {
            photoSidecars.addSidecar(entryName, sidecar);
            return;
        }

        try {
            int routeCountBefore = context.getRoutes().size();
            context.parse(new ByteArrayInputStream(bytes), context.getStartDate(), Files.getExtension(entryName));
            if (PhotoSidecars.isPhoto(entryName)) {
                List<BaseRoute<?, ?>> routes = context.getRoutes();
                photoSidecars.addPhotoRoutes(entryName, routes.subList(routeCountBefore, routes.size()));
            }
        } catch (Exception | OutOfMemoryError e) {
            // one entry that no format can read must not end reading the archive
            log.info("Cannot read zip entry " + entryName + ": " + e);
        }
    }

    public void write(BaseRoute<?, ?> route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}
