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

package slash.navigation.hgt;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Encapsulates access to HGT files.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class HgtFiles {
    private Map<Integer, ElevationTile> tileCache = new HashMap<Integer, ElevationTile>();
    private HgtFileCache fileCache = new HgtFileCache();
    private Map<String, RandomAccessFile> randomAccessFileCache = new HashMap<String, RandomAccessFile>();
    private HgtFileDownloader downloader;

    public HgtFiles() {
        downloader = new HgtFileDownloader(fileCache);
    }

    Integer createTileKey(double longitude, double latitude) {
        int longitudeAsInteger = (int)(longitude + 180.0);   // values from -180 to +180: 0 - 360
        int latitudeAsInteger = (int)(latitude + 90.0);      // values from  -90 to  +90: 0 - 180
        return longitudeAsInteger + latitudeAsInteger * 100000;
    }

    String createFileKey(double longitude, double latitude) {
        int longitudeAsInteger = (int) longitude;
        int latitudeAsInteger = (int) latitude;
        return format("%s%02d%s%03d.hgt", (latitude < 0) ? "S" : "N",
                (latitude < 0) ? ((latitudeAsInteger - 1) * -1) : latitudeAsInteger,
                (longitude < 0) ? "W" : "E",
                (longitude < 0) ? ((longitudeAsInteger - 1) * -1) : longitudeAsInteger);
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Integer tileKey = createTileKey(longitude, latitude);
        ElevationTile tile = tileCache.get(tileKey);
        if (tile == null) {

            String fileKey = createFileKey(longitude, latitude);
            RandomAccessFile randomAccessFile = randomAccessFileCache.get(fileKey);
            if (randomAccessFile == null) {

                File file = fileCache.get(fileKey);
                if (file == null) {
                    file = downloader.download(fileKey);
                    if (file == null)
                        return null;
                    fileCache.put(fileKey, file);
                }

                randomAccessFile = new RandomAccessFile(file, "r");
                randomAccessFileCache.put(fileKey, randomAccessFile);
            }

            tile = new ElevationTile(randomAccessFile);
            tileCache.put(tileKey, tile);
        }
        return tile.getElevationFor(longitude, latitude);
    }

    public void dispose() {
        for (RandomAccessFile randomAccessFile : randomAccessFileCache.values())
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot close random access file" + randomAccessFile);
            }
        randomAccessFileCache.clear();
        tileCache.clear();
    }
}
