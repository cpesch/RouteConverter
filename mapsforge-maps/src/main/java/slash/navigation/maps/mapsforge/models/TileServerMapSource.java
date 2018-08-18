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

import net.andreinc.aleph.AlephFormatter;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;
import slash.common.helpers.APIKeyRegistry;
import slash.navigation.maps.tileserver.TileServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 * A {@link OnlineTileSource} that is configured from a {@link TileServer}.
 *
 * @author Christian Pesch
 */

public class TileServerMapSource extends AbstractTileSource {
    private static final Preferences preferences = Preferences.userNodeForPackage(TileServerMapSource.class);
    private static final String PARALLEL_REQUEST_LIMIT_PREFERENCE = "parallelRequestLimit";
    private static final String THUNDER_FOREST_API_KEY = APIKeyRegistry.getInstance().getAPIKey("thunderforest", "map");
    private final TileServer tileServer;
    private boolean alpha = false;

    private static String[] getHostNames(TileServer tileServer) {
        String[] hostNames = tileServer.getHostNames().toArray(new String[0]);
        if (hostNames.length == 0)
            hostNames = new String[]{"not.existing.tile.server"};
        return hostNames;
    }

    public TileServerMapSource(TileServer tileServer) {
        super(getHostNames(tileServer), 80);
        this.tileServer = tileServer;
        setUserAgent("RouteConverter Map Client/" + System.getProperty("rest", "2.24"));
    }

    public int getParallelRequestsLimit() {
        return preferences.getInt(PARALLEL_REQUEST_LIMIT_PREFERENCE, 8);
    }

    public byte getZoomLevelMin() {
        return (byte)tileServer.getMinZoom();
    }

    public byte getZoomLevelMax() {
        return (byte)tileServer.getMaxZoom();
    }

    public boolean hasAlpha() {
        return alpha;
    }

    public void setAlpha(boolean alpha) {
        this.alpha = alpha;
    }

    public URL getTileUrl(Tile tile) throws MalformedURLException {
        // Integer.toString() avoids points that group digits
        String url = AlephFormatter.str(tileServer.getUrlPattern())
                .arg("hostname", getHostName())
                .arg("tilex", Integer.toString(tile.tileX))
                .arg("tiley", Integer.toString(tile.tileY))
                .arg("zoom", Integer.toString(tile.zoomLevel))
                .fmt();
        if (THUNDER_FOREST_API_KEY != null && tileServer.getCopyright().toLowerCase().contains("thunderforest"))
            url += ("?apikey=" + THUNDER_FOREST_API_KEY);
        return new URL(url);
    }
}
