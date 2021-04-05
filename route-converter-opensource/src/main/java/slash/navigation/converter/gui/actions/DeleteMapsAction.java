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

package slash.navigation.converter.gui.actions;

import slash.navigation.common.BoundingBox;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.RemoteMap;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * {@link Action} that displays a {@link LocalMap} from the {@link MapsforgeMapManager}.
 *
 * @author Christian Pesch
 */

public class DeleteMapsAction extends FrameAction {
    private final JTable table;
    private final MapsforgeMapManager mapManager;

    public DeleteMapsAction(JTable table, MapsforgeMapManager mapManager) {
        this.table = table;
        this.mapManager = mapManager;
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0)
            return;
        final List<LocalMap> selectedMaps = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            LocalMap map = mapManager.getAvailableMapsModel().getItem(table.convertRowIndexToModel(selectedRow));
            selectedMaps.add(map);
        }
        mapManager.delete(selectedMaps);
    }
}