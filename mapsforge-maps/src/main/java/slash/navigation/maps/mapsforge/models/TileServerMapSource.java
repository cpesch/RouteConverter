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

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;
import slash.common.helpers.APIKeyRegistry;
import slash.navigation.maps.tileserver.TileServer;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A {@link OnlineTileSource} that is configured from a {@link TileServer}.
 *
 * @author Christian Pesch
 */

public class TileServerMapSource extends OnlineTileSource {
    private static String apiKey = APIKeyRegistry.getInstance().getAPIKey("thunderforest", "map");
    private TileServer tileServer;

    private static String[] getHostNames(TileServer tileServer) {
        String[] hostNames = tileServer.getHostNames().toArray(new String[0]);
        if (hostNames.length == 0)
            hostNames = new String[]{"not.existing.tile.server"};
        return hostNames;
    }

    public TileServerMapSource(TileServer tileServer) {
        super(getHostNames(tileServer), 80);
        this.tileServer = tileServer;
        setName(tileServer.getId());
        setBaseUrl(tileServer.getBaseUrl());
        setExtension(tileServer.getExtension());
        setZoomLevelMin((byte) tileServer.getMinZoom());
        setZoomLevelMax((byte) tileServer.getMaxZoom());
        setUserAgent("RouteConverter Map Client/" + System.getProperty("rest", "2.24"));
    }

    public URL getTileUrl(Tile tile) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder(32);

        stringBuilder.append(getBaseUrl());
        stringBuilder.append(tile.zoomLevel);
        stringBuilder.append('/');
        stringBuilder.append(tile.tileX);
        stringBuilder.append('/');
        stringBuilder.append(tile.tileY);
        if (getExtension() != null)
            stringBuilder.append(getExtension());
        if (apiKey != null && tileServer.getCopyright().toLowerCase().contains("thunderforest"))
            stringBuilder.append("?apikey=").append(apiKey);

        return new URL(getProtocol(), getHostName(), port, stringBuilder.toString());
    }
}
