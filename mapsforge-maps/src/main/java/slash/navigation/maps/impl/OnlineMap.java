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
package slash.navigation.maps.impl;

import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import slash.navigation.common.BoundingBox;
import slash.navigation.maps.LocalMap;

import java.io.File;

/**
 * A {@link LocalMap} that is downloaded on request from an online service.
 *
 * @author Christian Pesch
 */

public class OnlineMap extends LocaleResourceImpl implements LocalMap {
    private final AbstractTileSource tileSource;

    public OnlineMap(String description, String url, AbstractTileSource tileSource) {
        super(description, url);
        this.tileSource = tileSource;
    }

    public BoundingBox getBoundingBox() {
        throw new UnsupportedOperationException();
    }

    public File getFile() {
        throw new UnsupportedOperationException();
    }

    public AbstractTileSource getTileSource() {
        return tileSource;
    }

    public boolean isVector() {
        return false;
    }
}
