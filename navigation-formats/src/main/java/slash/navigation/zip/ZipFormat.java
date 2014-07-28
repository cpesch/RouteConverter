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
import slash.common.io.NotClosingUnderlyingInputStream;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static slash.navigation.base.NavigationFormats.getReadFormatsPreferredByExtension;

/**
 * Reads routes from ZIP Archive (.zip) files.
 *
 * @author Christian Pesch
 */

public class ZipFormat extends BaseNavigationFormat<BaseRoute> {
    private static final Logger log = Logger.getLogger(ZipFormat.class.getName());
    static {
        System.setProperty("sun.zip.encoding", "default");
    }

    public String getExtension() {
        return ".zip";
    }

    public String getName() {
        return "ZIP Archive (" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
        throw new UnsupportedOperationException();
    }

    public boolean isWritingRouteCharacteristics() {
        throw new UnsupportedOperationException();
    }

    public <P extends NavigationPosition> BaseRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, CompactCalendar startDate, ParserContext<BaseRoute> parserContext) throws Exception {
        ZipInputStream zip = new ZipInputStream(source);
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                NotClosingUnderlyingInputStream buffer = new NotClosingUnderlyingInputStream(new BufferedInputStream(zip));
                int size = (int) entry.getSize() + 1;
                buffer.mark(size);
                List<NavigationFormat> formats = getReadFormatsPreferredByExtension(Files.getExtension(entry.getName()));
                parserContext.parse(buffer, startDate, formats);
                zip.closeEntry();
            }
        } catch (IOException e) {
            log.fine("Error reading invalid zip entry from " + source + ": " + e);
        } finally {
            try {
                zip.close();
            } catch (IOException e) {
                log.fine("Error closing zip from " + source + ": " + e);
            }
        }
    }

    public void write(BaseRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}
