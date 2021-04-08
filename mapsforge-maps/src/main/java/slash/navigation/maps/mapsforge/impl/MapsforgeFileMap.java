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

import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapType;

import java.io.File;
import java.io.IOException;

import static slash.navigation.maps.mapsforge.MapType.Mapsforge;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;

/**
 * A {@link LocalMap} that is rendered from a locally stored
 * <a href="https://github.com/mapsforge/mapsforge/blob/master/docs/Specification-Binary-Map-File.md">Mapsforge Binary Map</a>.
 *
 * @author Christian Pesch
 */

public class MapsforgeFileMap extends LocaleResourceImpl implements LocalMap {
    private final File file;
    private final String provider;
    private NotClosingMapFile mapFile = null;

    public MapsforgeFileMap(String description, String url, File file, String provider, String copyrightText) {
        super(description, url, copyrightText);
        this.file = file;
        this.provider = provider;
    }

    public MapType getType() {
        return Mapsforge;
    }

    public String getProvider() {
        return provider;
    }

    public synchronized MapFile getMapFile() {
        if(mapFile == null) {
            mapFile = new NotClosingMapFile(file);
        }
        return mapFile;
    }

    public Integer getZoomLevelMin() {
        return null;
    }

    public Integer getZoomLevelMax() {
        return null;
    }

    public BoundingBox getBoundingBox() {
        return toBoundingBox(getMapFile().boundingBox());
    }

    public synchronized void close() {
        if(mapFile != null) {
            mapFile.destroy();
            mapFile = null;
        }
    }

    public void delete() throws IOException {
        close();
        if(!file.delete())
            throw new IOException("Cannot delete " + file);
    }
}
