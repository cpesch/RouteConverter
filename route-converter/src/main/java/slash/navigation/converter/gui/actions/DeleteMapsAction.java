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

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.gui.actions.DialogAction;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * {@link Action} that deletes {@link LocalMap}s.
 *
 * @author Christian Pesch
 */

public class DeleteMapsAction extends DialogAction {
    private final JTable table;
    private final MapsforgeMapManager mapManager;

    public DeleteMapsAction(JDialog dialog, JTable table, MapsforgeMapManager mapManager) {
        super(dialog);
        this.table = table;
        this.mapManager = mapManager;
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0)
            return;

        List<LocalMap> selectedMaps = new ArrayList<>();
        StringBuilder mapNames = new StringBuilder();
        for (int i = 0; i < selectedRows.length; i++) {
            LocalMap map = mapManager.getAvailableMapsModel().getItem(table.convertRowIndexToModel(selectedRows[i]));
            selectedMaps.add(map);

            mapNames.append(map.getUrl());
            if (i < selectedRows.length - 1)
                mapNames.append(", ");
        }

        int confirm = showConfirmDialog(getDialog(), format(RouteConverter.getBundle().getString("confirm-delete-maps"), mapNames),
                getDialog().getTitle(), YES_NO_OPTION);
        if (confirm != YES_OPTION)
            return;

        try {
            mapManager.delete(selectedMaps);
        } catch (Exception e) {
            invokeLater(() -> {
                showMessageDialog(getDialog(), format(RouteConverter.getBundle().getString("cannot-delete-maps"), e), getDialog().getTitle(), ERROR_MESSAGE);
            });
        }
    }
}
