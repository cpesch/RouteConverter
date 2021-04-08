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
package slash.navigation.maps.mapsforge.impl;

import slash.navigation.common.BoundingBox;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapType;
import slash.navigation.maps.mapsforge.mbtiles.MBTilesFile;

import java.io.File;
import java.io.IOException;

import static slash.navigation.maps.mapsforge.MapType.MBTiles;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;

/**
 * A {@link LocalMap} that is rendered from a locally stored
 * <a href="https://github.com/mapbox/mbtiles-spec">MBTiles</a> SQLite database file.
 *
 * @author Christian Pesch
 */

public class MBTilesFileMap extends LocaleResourceImpl implements LocalMap {
    private final File file;
    private final String provider;
    private MBTilesFile mbTilesFile = null;

    public MBTilesFileMap(String description, String url, File file, String provider, String copyrightText) {
        super(description, url, copyrightText);
        this.file = file;
        this.provider = provider;
    }

    public MapType getType() {
        return MBTiles;
    }

    public String getProvider() {
        return provider;
    }

    public synchronized MBTilesFile getMBTilesFile() {
        if (mbTilesFile == null) {
            mbTilesFile = new MBTilesFile(file);
        }
        return mbTilesFile;
    }

    public Integer getZoomLevelMin() {
        return getMBTilesFile().getZoomLevelMin();
    }

    public Integer getZoomLevelMax() {
        return getMBTilesFile().getZoomLevelMax();
    }

    public BoundingBox getBoundingBox() {
        return toBoundingBox(getMBTilesFile().getBoundingBox());
    }

    public synchronized void close() {
        if(mbTilesFile != null) {
            mbTilesFile.close();
            mbTilesFile = null;
        }
    }

    public void delete() throws IOException {
        close();
        if(!file.delete())
            throw new IOException("Cannot delete " + file);
    }
}
