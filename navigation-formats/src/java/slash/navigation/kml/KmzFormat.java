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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.util.InputOutput;
import slash.navigation.util.NotClosingUnderlyingInputStream;
import slash.navigation.util.CompactCalendar;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.*;

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

    public int getMaximumPositionCount() {
        return delegate.getMaximumPositionCount();
    }

    public boolean isSupportsMultipleRoutes() {
        return delegate.isSupportsMultipleRoutes();
    }

    public <P extends BaseNavigationPosition> KmlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return delegate.createRoute(characteristics, name, positions);
    }

    public List<KmlRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        List<KmlRoute> result = new ArrayList<KmlRoute>();
        ZipInputStream zip = new ZipInputStream(source);
        try {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                try {
                    List<KmlRoute> routes = delegate.internalRead(new NotClosingUnderlyingInputStream(zip));
                    if (routes != null)
                        result.addAll(routes);
                } catch (JAXBException e) {
                    log.fine("Error reading " + entry + " from " + source + ": " + e.getMessage());
                }
                zip.closeEntry();
            }
            return result.size() > 0 ? result : null;
        }
        catch (ZipException e) {
            log.fine("Error reading zip entries from " + source + ": " + e.getMessage());
            return null;
        }
        finally {
          zip.close();
        }
    }

    private void writeIntermediate(File target, byte[] bytes) throws IOException {
        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(bytes);

        ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(target));
        try {
            ZipEntry entry = new ZipEntry("doc.kml");
            entry.setSize(bytes.length);
            entry.setCrc(crc.getValue());
            outputStream.putNextEntry(entry);
            outputStream.write(bytes, 0, bytes.length);
            outputStream.finish();
        }
        finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    public void write(KmlRoute route, File target, int startIndex, int endIndex) throws IOException {
        File intermediate = File.createTempFile("rckml", ".kml");

        try {
            delegate.write(route, intermediate, startIndex, endIndex);
            byte[] bytes = InputOutput.readBytes(new FileInputStream(intermediate));
            writeIntermediate(target, bytes);
        }
        finally {
            if (intermediate.exists()) {
                if (!intermediate.delete())
                    log.warning("Cannot delete intermediate file " + intermediate);
            }
        }
    }

    public void write(List<KmlRoute> routes, File target) throws IOException {
        File intermediate = File.createTempFile("rckml", ".kml");

        try {
            delegate.write(routes, intermediate);
            byte[] bytes = InputOutput.readBytes(new FileInputStream(intermediate));
            writeIntermediate(target, bytes);
        }
        finally {
            if (intermediate.exists())
                if (!intermediate.delete())
                    log.warning("Cannot delete intermediate file " + intermediate);
        }
    }
}
