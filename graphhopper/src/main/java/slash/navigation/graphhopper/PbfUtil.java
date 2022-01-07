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
package slash.navigation.graphhopper;

import org.openstreetmap.osmosis.osmbinary.Fileformat;
import org.openstreetmap.osmosis.osmbinary.Osmformat;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import static java.lang.String.format;
import static slash.common.io.Files.removeExtension;

/**
 * Provides PBF functionality.
 *
 * @author Christian Pesch
 */
public class PbfUtil {
    private static final Logger log = Logger.getLogger(PbfUtil.class.getName());
    private static final String OSM_HEADER = "OSMHeader";
    public static final String DOT_OSM = ".osm";
    public static final String DOT_PBF = ".pbf";
    private static final String LATEST = "-latest";
    public static final String PROPERTIES = "properties";
    private static final double LONGITUDE_LATITUDE_RESOLUTION = 1000.0 * 1000.0 * 1000.0;

    private static File createGraphDirectory(java.io.File file, boolean removeLatest) {
        String name = file.getName().replace(DOT_PBF, "").replace(DOT_OSM, "");
        if(removeLatest)
            name = name.replaceAll(LATEST, "");
        name = removeExtension(name);
        return new java.io.File(file.getParent(), name);
    }

    public static File lookupGraphDirectory(java.io.File file) {
        File directory = createGraphDirectory(file, false);
        if(!directory.exists())
            directory = createGraphDirectory(file, true);
        return directory;
    }

    public static File createPropertiesFile(java.io.File file) {
        return new File(lookupGraphDirectory(file), PROPERTIES);
    }

    public static boolean existsGraphDirectory(java.io.File file) {
        return createPropertiesFile(file).exists();
    }

    public static BoundingBox extractBoundingBox(File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return extractBoundingBox(inputStream);
        }
    }

    public static BoundingBox extractBoundingBox(InputStream inputStream) {
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            boolean foundOsmHeader = false;

            while (!foundOsmHeader) {
                if (dataInputStream.available() <= 0)
                    break;

                byte[] blobHeaderBytes = new byte[dataInputStream.readInt()];
                int readBlobHeader = dataInputStream.read(blobHeaderBytes);
                if (readBlobHeader != blobHeaderBytes.length) {
                    log.warning(format("Wanted to read %d blob header bytes, but got only %d bytes", blobHeaderBytes.length, readBlobHeader));
                    return null;
                }
                Fileformat.BlobHeader blobHeader = Fileformat.BlobHeader.parseFrom(blobHeaderBytes);

                byte[] blobBytes = new byte[blobHeader.getDatasize()];
                int readBlob = dataInputStream.read(blobBytes);
                if (readBlob != blobBytes.length) {
                    log.warning(format("Wanted to read %d blob bytes, but got only %d bytes", blobBytes.length, readBlob));
                    return null;
                }
                Fileformat.Blob blob = Fileformat.Blob.parseFrom(blobBytes);

                InputStream blobData;
                if (blob.hasZlibData()) {
                    blobData = new InflaterInputStream(blob.getZlibData().newInput());
                } else {
                    blobData = blob.getRaw().newInput();
                }

                if (blobHeader.getType().equals(OSM_HEADER)) {
                    Osmformat.HeaderBlock headerBlock = Osmformat.HeaderBlock.parseFrom(blobData);
                    if (headerBlock.hasBbox())
                        return toBoundingBox(headerBlock.getBbox());

                    foundOsmHeader = true;
                } else
                    log.info("Skipped block " + blobHeader.getType() + " with " + blobBytes.length + " bytes");
            }
        } catch (IOException e) {
            log.warning(format("Could not extract pbf bounding box: %s", e));
        }
        return null;
    }

    private static BoundingBox toBoundingBox(Osmformat.HeaderBBox bbox) {
        return new BoundingBox(new SimpleNavigationPosition(asCoordinate(bbox.getRight()), asCoordinate(bbox.getTop())),
                new SimpleNavigationPosition(asCoordinate(bbox.getLeft()), asCoordinate(bbox.getBottom())));
    }

    private static double asCoordinate(long coordinate) {
        return coordinate / LONGITUDE_LATITUDE_RESOLUTION;
    }

    public static void main(String[] args) throws IOException {
        for(String arg : args) {
            File file = new File(arg);
            log.info(format("File %s has bounding box %s", file, extractBoundingBox(file)));
        }
    }
}
