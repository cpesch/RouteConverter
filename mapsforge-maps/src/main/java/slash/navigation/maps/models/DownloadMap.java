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
package slash.navigation.maps.models;

import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import slash.navigation.maps.Map;

import java.io.File;

/**
 * A {@link Map} that is downloaded on request from an online service.
 *
 * @author Christian Pesch
 */

public class DownloadMap extends LocaleResourceImpl implements Map {
    private final AbstractTileSource tileSource;

    public DownloadMap(String description, String url, AbstractTileSource tileSource) {
        super(description, url);
        this.tileSource = tileSource;
    }

    public File getFile() {
        throw new UnsupportedOperationException();
    }

    public AbstractTileSource getTileSource() {
        return tileSource;
    }

    public boolean isRenderer() {
        return false;
    }
}
