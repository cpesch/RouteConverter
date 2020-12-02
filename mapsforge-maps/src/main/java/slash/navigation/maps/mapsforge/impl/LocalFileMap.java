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

import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import slash.navigation.common.BoundingBox;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapType;

import java.io.File;

/**
 * A {@link LocalMap} that is rendered from a locally stored
 * <a href="https://github.com/mapsforge/mapsforge/blob/master/docs/Specification-Binary-Map-File.md">Mapsforge Binary Map</a>
 * <a href="https://github.com/mapbox/mbtiles-spec">MBTiles</a> SQLite database file.
 *
 * @author Christian Pesch
 */

public class LocalFileMap extends LocaleResourceImpl implements LocalMap {
    private final File file;
    private final MapType mapType;
    private final BoundingBox boundingBox;

    public LocalFileMap(String description, String url, File file, MapType mapType, BoundingBox boundingBox, String copyrightText) {
        super(description, url, copyrightText);
        this.file = file;
        this.mapType = mapType;
        this.boundingBox = boundingBox;
    }

    public MapType getType() {
        return mapType;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public File getFile() {
        return file;
    }

    public AbstractTileSource getTileSource() {
        throw new UnsupportedOperationException();
    }
}
