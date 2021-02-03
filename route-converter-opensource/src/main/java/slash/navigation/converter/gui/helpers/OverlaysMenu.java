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
package slash.navigation.converter.gui.helpers;

import slash.navigation.converter.gui.actions.ToggleOverlayAction;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.tileserver.TileServer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.util.List;

/**
 * Creates a {@link JMenu} for {@link TileServer overlays}.
 *
 * @author Christian Pesch
 */

public class OverlaysMenu {
    private final JMenu menu;
    private final ItemTableModel<TileServer> availableOverlaysModel;
    private final ItemTableModel<TileServer> appliedOverlaysModel;

    public OverlaysMenu(JMenu menu, ItemTableModel<TileServer> availableOverlaysModel, ItemTableModel<TileServer> appliedOverlaysModel) {
        this.menu = menu;
        this.availableOverlaysModel = availableOverlaysModel;
        this.appliedOverlaysModel = appliedOverlaysModel;

        availableOverlaysModel.addTableModelListener(e -> {
            if (e.getType() != TableModelEvent.INSERT)
                return;
            addMenuEntry(e.getFirstRow());
        });

        appliedOverlaysModel.addTableModelListener(e -> enableMenuEntries());
    }

    private void addMenuEntry(int row) {
        List<TileServer> tileServers = availableOverlaysModel.getItems();
        TileServer tileServer = tileServers.get(row);

        JCheckBoxMenuItem item = new JCheckBoxMenuItem();
        item.setAction(new ToggleOverlayAction(appliedOverlaysModel, tileServer));
        item.setText(tileServer.getId());
        item.setToolTipText(tileServer.getDescription());
        menu.add(item);

        menu.setEnabled(tileServers.size() > 0);
    }

    private void enableMenuEntries() {
        List<TileServer> tileServers = availableOverlaysModel.getItems();
        for (int i = 0; i < tileServers.size(); i++) {
            TileServer tileServer = tileServers.get(i);
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) menu.getItem(i);
            boolean contains = appliedOverlaysModel.contains(tileServer);
            item.setState(contains);
        }
    }
}
