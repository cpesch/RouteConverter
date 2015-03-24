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
package slash.navigation.maps.helpers;

import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;

import java.io.*;

import static java.io.File.createTempFile;
import static java.lang.String.format;
import static slash.navigation.maps.helpers.MapTransfer.toBoundingBox;

/**
 * Provides map functionality.
 *
 * @author Christian Pesch
 */

public class MapUtil {
    public static BoundingBox extractBoundingBox(File file) {
        MapFile mapFile = new MapFile(file);
        org.mapsforge.core.model.BoundingBox boundingBox = mapFile.boundingBox();
        mapFile.close();
        return toBoundingBox(boundingBox);
    }

    public static BoundingBox extractBoundingBox(InputStream inputStream, long fileSize) throws IOException {
        File file = writePartialFile(inputStream, fileSize);
        BoundingBox result = extractBoundingBox(file);
        if (!file.delete())
            throw new IOException(format("Could not delete temporary map file '%s'", file));
        return result;
    }

    public static File writePartialFile(InputStream inputStream, long fileSize) throws IOException {
        File file = createTempFile("partialmap", ".tmp");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        byte[] buffer = new byte[1024];
        while (true) {
            try {
                int read = inputStream.read(buffer);
                if (read == -1)
                    break;
                raf.write(buffer, 0, read);
            } catch (EOFException e) {
                break;
            }
        }

        raf.setLength(fileSize);
        raf.close();
        return file;
    }
}
