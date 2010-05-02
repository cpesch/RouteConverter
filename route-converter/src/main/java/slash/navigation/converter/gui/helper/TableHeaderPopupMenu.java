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

import slash.navigation.gui.FrameAction;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsTableColumnModel;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a {@link JPopupMenu} for the {@link JTableHeader} of the {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class TableHeaderPopupMenu {
    private final List<JCheckBoxMenuItem> menuItems = new ArrayList<JCheckBoxMenuItem>();
    private final JPopupMenu popupMenu = new JPopupMenu();

    public TableHeaderPopupMenu(JTableHeader tableHeader, final PositionsTableColumnModel tableColumnModel) {
        for (final PositionTableColumn column : tableColumnModel.getPreparedColumns()) {
            String text = RouteConverter.getBundle().getString("show-column") + " " + RouteConverter.getBundle().getString(column.getName());
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text, column.isVisible());
            menuItem.addActionListener(new FrameAction() {
                public void run() {
                    tableColumnModel.toggleVisibility(column);

                    if (tableColumnModel.getVisibleColumnCount() > 1)
                        enableMenuItems();
                    else
                        disableLastSelectedMenuItem();
                }
            });
            menuItems.add(menuItem);
            popupMenu.add(menuItem);
        }

        tableHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
        });
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    private void enableMenuItems() {
        for (JCheckBoxMenuItem menuItem : menuItems) {
            if (!menuItem.isEnabled())
                menuItem.setEnabled(true);
        }
    }

    private void disableLastSelectedMenuItem() {
        for (JCheckBoxMenuItem menuItem : menuItems) {
            if (menuItem.isSelected())
                menuItem.setEnabled(false);
        }
    }

    private void showPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
