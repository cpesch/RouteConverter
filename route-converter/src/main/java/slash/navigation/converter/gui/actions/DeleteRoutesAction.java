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

import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.routes.impl.RouteModel;

import javax.swing.*;
import java.util.List;

import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.getSelectedRouteModels;
import static slash.navigation.gui.helpers.JTableHelper.selectAndScrollToPosition;

/**
 * {@link Action} that deletes {@link RouteModel}s from the {@link CatalogModel}.
 *
 * @author Christian Pesch
 */

public class DeleteRoutesAction extends FrameAction {
    private final JTable table;
    private final CatalogModel catalogModel;

    public DeleteRoutesAction(JTable table, CatalogModel catalogModel) {
        this.table = table;
        this.catalogModel = catalogModel;
    }

    public void run() {
        List<RouteModel> routes = getSelectedRouteModels(table);
        if(routes.size() == 0)
            return;

        int[] selectedRows = table.getSelectedRows();

        catalogModel.deleteRoutes(routes);

        final int deleteRow = selectedRows[0] < table.getRowCount() ?
                selectedRows[0] : table.getRowCount() - 1;
        if (table.getRowCount() > 0) {
            invokeLater(new Runnable() {
                public void run() {
                    selectAndScrollToPosition(table, deleteRow, deleteRow);
                }
            });
        }
    }
}