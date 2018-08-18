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

import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.tileserver.binding.TileServerType;
import slash.navigation.maps.tileserver.helpers.ItemPreferencesMediator;
import slash.navigation.maps.tileserver.helpers.TileServerService;

import java.io.File;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.io.Transfer.formatInt;

/**
 * Manages {@link TileServer}s
 *
 * @author Christian Pesch
 */

public class TileServerMapManager {
    private static final String APPLIED_OVERLAY_PREFERENCE = "appliedOverlay";
    private final TileServerService tileServerService;
    private ItemTableModel<TileServer> availableMapsModel = new ItemTableModel<>(1);
    private ItemTableModel<TileServer> availableOverlaysModel = new ItemTableModel<>(1);
    private ItemTableModel<TileServer> appliedOverlaysModel = new ItemTableModel<>(1);
    private ItemPreferencesMediator itemPreferencesMediator;

    public TileServerMapManager(File tileServerDirectory) {
        this.tileServerService = new TileServerService(tileServerDirectory);
        initializeOnlineMaps();
        itemPreferencesMediator = new ItemPreferencesMediator<TileServer>(availableOverlaysModel, appliedOverlaysModel, APPLIED_OVERLAY_PREFERENCE) {
            protected String itemToString(TileServer tileServer) {
                return tileServer.getId();
            }
        };
    }

    public void dispose() {
        itemPreferencesMediator.dispose();
        itemPreferencesMediator = null;
    }

    public ItemTableModel<TileServer> getAvailableMapsModel() {
        return availableMapsModel;
    }

    public ItemTableModel<TileServer> getAvailableOverlaysModel() {
        return availableOverlaysModel;
    }

    public ItemTableModel<TileServer> getAppliedOverlaysModel() {
        return appliedOverlaysModel;
    }

    private void initializeOnlineMaps() {
        availableMapsModel.clear();
        availableOverlaysModel.clear();
    }

    public void scanTileServers() {
        tileServerService.initialize();

        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                for (TileServerType type : tileServerService.getTileServers())
                    availableMapsModel.addOrUpdateItem(new TileServer(type.getId(), type.getName(),
                            // TODO fix me once the XML is different
                            "http://#{hostname}" + type.getBaseUrl() + "#{zoom}/#{tilex}/#{tiley}" + type.getExtension(),
                            type.getHostName(),
                            type.getActive() == null || type.getActive(),
                            formatInt(type.getMinZoom()), formatInt(type.getMaxZoom()),
                            type.getCopyright() != null ? type.getCopyright().value() : "Unknown"
         ));

                for (TileServerType type : tileServerService.getOverlays())
                    availableOverlaysModel.addOrUpdateItem(new TileServer(type.getId(), type.getName(),
                            // TODO fix me once the XML is different
                            "http://#{hostname}" + type.getBaseUrl() + "#{zoom}/#{tilex}/#{tiley}" + type.getExtension(),
                            type.getHostName(),
                            type.getActive() == null || type.getActive(),
                            formatInt(type.getMinZoom()), formatInt(type.getMaxZoom()),
                            type.getCopyright() != null ? type.getCopyright().value() : "Unknown"));
            }
        });
    }
}
