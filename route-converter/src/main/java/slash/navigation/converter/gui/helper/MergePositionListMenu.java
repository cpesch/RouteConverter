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

package slash.navigation.converter.gui.helper;

import slash.navigation.base.BaseRoute;
import slash.navigation.converter.gui.actions.MergePositionList;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsTableColumnModel;
import slash.navigation.util.RouteComments;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Updates a {@link JMenu} with the position lists from {@link PositionsTableColumnModel}.
 *
 * @author Christian Pesch
 */

public class MergePositionListMenu {
    private JMenu menu;
    private FormatAndRoutesModel formatAndRoutesModel;

    public MergePositionListMenu(JMenu menu, JTable table, FormatAndRoutesModel formatAndRoutesModel) {
        this.menu = menu;
        this.formatAndRoutesModel = formatAndRoutesModel;
        initialize(table);
    }

    private void initialize(final JTable table) {
        formatAndRoutesModel.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    BaseRoute route = formatAndRoutesModel.getRoute(i);
                    JMenuItem menuItem = new JMenuItem(new MergePositionList(table, route, formatAndRoutesModel));
                    menuItem.setText(RouteComments.shortenRouteName(route));
                    menu.add(menuItem, i);
                }
            }

            public void intervalRemoved(ListDataEvent e) {
                for (int i = e.getIndex1(); i >= e.getIndex0(); i--) {
                    JMenuItem menuItem = i < menu.getMenuComponentCount() ? (JMenuItem) menu.getMenuComponent(i) : null;
                    if (menuItem != null) {
                        menuItem.setAction(null);
                    }
                    menu.remove(i);
                }
            }

            public void contentsChanged(ListDataEvent e) {
                for (int i = e.getIndex0(); i <= e.getIndex1(); i++) {
                    if (i >= 0 && i < menu.getMenuComponentCount()) {
                        BaseRoute route = formatAndRoutesModel.getRoute(i);
                        JMenuItem menuItem = (JMenuItem) menu.getMenuComponent(i);
                        menuItem.setText(RouteComments.shortenRouteName(route));
                    }
                }
            }
        });
    }
}
