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

import slash.common.io.Files;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads routes from ZIP Archive (.zip) files.
 *
 * @author Christian Pesch
 */

public class ZipFormat extends BaseNavigationFormat<BaseRoute<?, ?>> {
    private static final Logger log = Logger.getLogger(ZipFormat.class.getName());
    static final int MAXIMUM_ENTRY_SIZE = 256 * 1024 * 1024;
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
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;

                // read the whole entry so that every format probe can reset() to its start: streamed
                // archives like Google Takeout carry the size in a data descriptor after the data, so
                // entry.getSize() is -1 and a mark() derived from it broke reset() after 8 KB
                byte[] bytes = zip.readNBytes(MAXIMUM_ENTRY_SIZE + 1);
                if (bytes.length > MAXIMUM_ENTRY_SIZE) {
                    log.warning("Skipping zip entry " + entry.getName() + " larger than " + MAXIMUM_ENTRY_SIZE + " bytes");
                    zip.closeEntry();
                    continue;
                }

                try {
                    context.parse(new ByteArrayInputStream(bytes), context.getStartDate(), Files.getExtension(entry.getName()));
                } catch (Exception e) {
                    // one entry that no format can read must not end reading the archive
                    log.fine("Cannot read zip entry " + entry.getName() + ": " + e);
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            log.fine("Error reading invalid zip entry from " + source + ": " + e);
        }
    }

    public void write(BaseRoute<?, ?> route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}
