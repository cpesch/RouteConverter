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
package slash.navigation.maps.tileserver;

import slash.navigation.maps.item.Item;

import java.util.List;

import static slash.common.io.Files.printArrayToDialogString;

/**
 * Description of a tile server that serves an online map.
 *
 * @author Christian Pesch
 */

public class TileServer implements Item {
    private final String id;
    private final String description;
    private final String urlPattern;
    private final List<String> hostNames;
    private final boolean active;
    private final int minZoom;
    private final int maxZoom;
    private final String copyright;

    public TileServer(String id, String description, String urlPattern, List<String> hostNames,
                      boolean active, int minZoom, int maxZoom, String copyright) {
        this.id = id;
        this.description = description;
        this.urlPattern = urlPattern;
        this.hostNames = hostNames;
        this.active = active;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        this.copyright = copyright;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return printArrayToDialogString(getHostNames().toArray(), false);
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public List<String> getHostNames() {
        return hostNames;
    }

    public boolean isActive() {
        return active;
    }

    public int getMinZoom() {
        return minZoom;
    }

    public int getMaxZoom() {
        return maxZoom;
    }

    public String getCopyright() {
        return copyright;
    }
}
