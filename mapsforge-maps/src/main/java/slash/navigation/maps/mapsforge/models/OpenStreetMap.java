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
package slash.navigation.maps.mapsforge.models;

import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.impl.TileDownloadMap;

import static slash.navigation.maps.tileserver.TileServerMapManager.retrieveCopyrightText;

/**
 * Default {@link LocalMap} to display a map in the Opensource Edition without any downloads.
 *
 * @author Christian Pesch
 */

public class OpenStreetMap extends TileDownloadMap {
    public static final String OPENSTREETMAP_URL = "http://www.openstreetmap.org/";

    public OpenStreetMap() {
        super("OpenStreetMap Default Map", OPENSTREETMAP_URL, true, new RouteConverterOpenStreetMapMapnik(), retrieveCopyrightText("OpenStreetMap"));
    }

    /**
     * mapsforge's tile source for tile.openstreetmap.org that identifies as RouteConverter, resolved on every
     * request so that it carries the version even when created before the "rest" system property is set.
     */
    private static class RouteConverterOpenStreetMapMapnik extends OpenStreetMapMapnik {
        RouteConverterOpenStreetMapMapnik() {
            super(new String[]{"tile.openstreetmap.org"}, 443);
        }

        public String getUserAgent() {
            return TileServerMapSource.userAgent();
        }
    }
}
