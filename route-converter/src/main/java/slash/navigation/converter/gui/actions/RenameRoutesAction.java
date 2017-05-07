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
import slash.navigation.gui.helpers.WindowHelper;
import slash.navigation.routes.impl.RouteModel;
import slash.navigation.routes.impl.RoutesTableModel;

import javax.swing.*;
import java.util.List;

import static java.text.MessageFormat.format;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.showInputDialog;
import static slash.common.io.Transfer.trim;
import static slash.navigation.converter.gui.helpers.RouteHelper.formatName;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.getSelectedRouteModels;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.selectRoute;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;

/**
 * {@link Action} that renames a {@link RouteModel} of the {@link CatalogModel}.
 *
 * @author Christian Pesch
 */

public class RenameRoutesAction extends FrameAction {
    private final JTable table;
    private final CatalogModel catalogModel;

    public RenameRoutesAction(JTable table, CatalogModel catalogModel) {
        this.table = table;
        this.catalogModel = catalogModel;
    }

    public void run() {
        List<RouteModel> routes = getSelectedRouteModels(table);
        if (routes.size() == 0)
            return;

        for (final RouteModel route : routes) {
            String name = (String) showInputDialog(WindowHelper.getFrame(),
                    format(getBundle().getString("rename-route-label"), formatName(route)),
                    WindowHelper.getFrame().getTitle(), QUESTION_MESSAGE, null, null, route.getDescription());
            if (trim(name) == null)
                return;

            catalogModel.renameRoute(route, name, new Runnable() {
                public void run() {
                    final int row = ((RoutesTableModel) table.getModel()).getIndex(route);
                    scrollToPosition(table, row);
                    selectRoute(table, route);
                }
            });
        }
    }
}