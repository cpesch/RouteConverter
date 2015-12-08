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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static java.io.File.createTempFile;
import static java.io.File.separator;
import static java.lang.String.format;
import static slash.common.io.Files.writePartialFile;
import static slash.navigation.maps.helpers.MapTransfer.toBoundingBox;

/**
 * Provides map functionality.
 *
 * @author Christian Pesch
 */

public class MapUtil {
    private static final Logger log = Logger.getLogger(MapUtil.class.getName());

    public static BoundingBox extractBoundingBox(File file) {
        try {
            MapFile mapFile = new MapFile(file);
            org.mapsforge.core.model.BoundingBox boundingBox = mapFile.boundingBox();
            mapFile.close();
            return toBoundingBox(boundingBox);
        } catch (Exception e) {
            log.warning(format("Could not extract mapsforge bounding box from %s: %s", file, e));
        }
        return null;
    }

    public static BoundingBox extractBoundingBox(InputStream inputStream, long fileSize) {
        try {
            File file = createTempFile("partialmap", ".map");
            writePartialFile(inputStream, fileSize, file);
            BoundingBox result = extractBoundingBox(file);
            if (!file.delete())
                throw new IOException(format("Could not delete temporary partial map file '%s'", file));
            return result;
        } catch (Exception e) {
            log.warning(format("Could not extract mapsforge bounding box: %s", e));
        }
        return null;
    }

    public static String removePrefix(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(rootPath))
            filePath = filePath.substring(rootPath.length());
        else
            filePath = file.getName();
        if (filePath.startsWith(separator))
            filePath = filePath.substring(1);
        filePath = filePath.replace(separator, "/");
        return filePath;
    }
}
