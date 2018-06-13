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

import slash.navigation.maps.tileserver.binding.TileServerType;
import slash.navigation.maps.tileserver.helpers.TileServerService;
import slash.navigation.maps.tileserver.item.ItemTableModel;

import java.io.File;
import java.util.List;

import static slash.common.io.Transfer.formatInt;

/**
 * Manages {@link TileServer}s
 *
 * @author Christian Pesch
 */

public class TileServerMapManager {
    private final TileServerService tileServerService;
    private ItemTableModel<TileServer> availableMapsModel = new ItemTableModel<>(1);

    public TileServerMapManager(File tileServerDirectory) {
        this.tileServerService = new TileServerService(tileServerDirectory);
        initializeOnlineMaps();
    }

    public ItemTableModel<TileServer> getAvailableMapsModel() {
        return availableMapsModel;
    }

    private void initializeOnlineMaps() {
        availableMapsModel.clear();
    }

    public void scanTileServers() {
        tileServerService.initialize();

        for(TileServerType type : tileServerService.getTileServers())
          availableMapsModel.addOrUpdateItem(new TileServer(type.getId(), type.getName(), type.getHostName(),
                  type.getBaseUrl(), type.getExtension(), type.getActive() == null || type.getActive(),
                  formatInt(type.getMinZoom()), formatInt(type.getMaxZoom()),
                  type.getCopyright() != null ? type.getCopyright().value() : "Unknown"));
    }

    public List<TileServer> getTileServers() {
        return availableMapsModel.getItems();
    }
}
