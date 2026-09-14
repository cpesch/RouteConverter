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
import slash.navigation.maps.tileserver.TileServer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * A {@link OnlineTileSource} that is configured from a {@link TileServer}.
 *
 * @author Christian Pesch
 */

public class TileServerMapSource extends AbstractTileSource {
    private static final Preferences preferences = Preferences.userNodeForPackage(TileServerMapSource.class);
    private static final String PARALLEL_REQUEST_LIMIT_PREFERENCE = "parallelRequestLimit";
    private static final String OUTDOOR_ACTIVE_TILE_DOMAIN = "oastatic.com";
    private final TileServer tileServer;
    private boolean alpha = false;

    private static String[] getHostNames(TileServer tileServer) {
        String[] hostNames = tileServer.hosts().toArray(new String[0]);
        if (hostNames.length == 0)
            hostNames = new String[]{"not.existing.tile.server"};
        return hostNames;
    }

    public TileServerMapSource(TileServer tileServer) {
        this(tileServer, getHostNames(tileServer));
    }

    private TileServerMapSource(TileServer tileServer, String[] hostNames) {
        super(hostNames, 80);
        this.tileServer = tileServer;
        setUserAgent(userAgent(hostNames));
        setTimeoutConnect(30 * 1000);
        setTimeoutRead(120 * 1000);
    }

    /**
     * Identify compliantly to tile providers (esp. OpenStreetMap's usage policy); never masquerade as a browser.
     * <p>
     * Exception for OutdoorActive: their tile servers answer 404 to every request whose User-Agent contains
     * "routeconverter" (in the name or in the +URL) or "osmand", while serving any other client - browsers,
     * competing apps and plain wget alike (measured 2026-09-14). That left the "OAC Summer" and "OSM Summer"
     * maps blank, so for those hosts we send the JDK's own default identification instead of ours.
     */
    static String userAgent(String[] hostNames) {
        for (String hostName : hostNames)
            if (isOutdoorActiveHost(hostName))
                return "Java/" + System.getProperty("java.version", "21");
        return "RouteConverter/" + System.getProperty("rest", "3.0") + " (+https://www.routeconverter.com/)";
    }

    private static boolean isOutdoorActiveHost(String hostName) {
        String lowerCase = hostName.toLowerCase();
        return lowerCase.equals(OUTDOOR_ACTIVE_TILE_DOMAIN) || lowerCase.endsWith("." + OUTDOOR_ACTIVE_TILE_DOMAIN);
    }

    public int getParallelRequestsLimit() {
        return preferences.getInt(PARALLEL_REQUEST_LIMIT_PREFERENCE, 8);
    }

    public byte getZoomLevelMin() {
        return (byte)tileServer.minZoom();
    }

    public byte getZoomLevelMax() {
        return (byte)tileServer.maxZoom();
    }

    public boolean hasAlpha() {
        return alpha;
    }

    public void setAlpha(boolean alpha) {
        this.alpha = alpha;
    }

    static String appendApiKey(String url, String apiKey, boolean mapbox) {
        if (apiKey == null)
            return url;
        String param = mapbox ? "access_token" : "apikey";
        return url + (url.contains("?") ? "&" : "?") + param + "=" + apiKey;
    }

    public URL getTileUrl(Tile tile) throws MalformedURLException {
        // Integer.toString() avoids points that group digits
        String url = AlephFormatter.str(tileServer.urlPattern())
                .arg("host", getHostName())
                .arg("language", Locale.getDefault().getLanguage())
                .arg("tilex", Integer.toString(tile.tileX))
                .arg("tiley", Integer.toString(tile.tileY))
                .arg("zoom", Integer.toString(tile.zoomLevel))
                .fmt();
        url = appendApiKey(url, getApiKey(), tileServer.copyright().toLowerCase().contains("mapbox"));
        return URI.create(url).toURL();
    }
}
