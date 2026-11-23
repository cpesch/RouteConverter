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

import static slash.navigation.base.RouteComments.createRouteName;

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
        int archiveRouteCountBefore = context.getRoutes().size();
        PhotoSidecars photoSidecars = new PhotoSidecars();
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

                String entryName = entry.getName();
                String photoName = PhotoSidecars.photoNameOfSidecar(entryName);
                if (photoName != null) {
                    photoSidecars.addSidecar(photoName, bytes);
                    zip.closeEntry();
                    continue;
                }

                try {
                    int routeCountBefore = context.getRoutes().size();
                    context.parse(new ByteArrayInputStream(bytes), context.getStartDate(), Files.getExtension(entryName));
                    List<BaseRoute<?, ?>> routes = context.getRoutes();
                    List<BaseRoute<?, ?>> entryRoutes = routes.subList(routeCountBefore, routes.size());
                    nameAfterEntry(entryRoutes, entryName, context);
                    if (PhotoSidecars.isPhoto(entryName))
                        photoSidecars.addPhotoRoutes(entryName, entryRoutes);
                } catch (Exception e) {
                    // one entry that no format can read must not end reading the archive
                    log.fine("Cannot read zip entry " + entry.getName() + ": " + e);
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            log.fine("Error reading invalid zip entry from " + source + ": " + e);
        }
        photoSidecars.removePhotosWithoutCoordinates(context);

        // no entry had routes: decline, since a ZIP archive cannot stand for an empty position list
        if (context.getRoutes().size() == archiveRouteCountBefore)
            throw new IllegalArgumentException("No routes in ZIP archive " + source);
    }

    /**
     * Names the position lists of an entry after the entry when the format found no name or,
     * reading from the archive's file, took the archive's name: the daily GPS files of a Google
     * Takeout archive become gps_location_2024-01-09 instead of an unnamed list
     */
    static void nameAfterEntry(List<BaseRoute<?, ?>> routes, String entryName, ParserContext<BaseRoute<?, ?>> context) {
        String archiveName = context.getFile() != null ? removeExtension(context.getFile().getName()) : null;
        String name = removeExtension(entryName.substring(entryName.lastIndexOf('/') + 1));
        for (BaseRoute<?, ?> route : routes) {
            String routeName = route.getName();
            // CSV and other formats derive "first to last" from the descriptions when they have no name
            if (routeName == null || routeName.isBlank() || routeName.equals(archiveName) ||
                    routeName.equals(createRouteName(route.getPositions())))
                route.setName(name);
        }
    }

    private static String removeExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public void write(BaseRoute<?, ?> route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}
