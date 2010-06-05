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

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.ToggleColumnVisibilityAction;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionTableColumnButtonModel;
import slash.navigation.converter.gui.models.PositionsTableColumnModel;
import slash.navigation.gui.ActionManager;
import slash.navigation.gui.Application;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a {@link JMenu} and a {@link JPopupMenu} for a {@link PositionsTableColumnModel}.
 *
 * @author Christian Pesch
 */

public class TableHeaderMenu {
    private JPopupMenu popupMenu = new JPopupMenu();
    private PositionsTableColumnModel columnModel;

    public TableHeaderMenu(JTableHeader tableHeader, JMenuBar menuBar, PositionsTableColumnModel columnModel) {
        this.columnModel = columnModel;
        initialize(tableHeader, JMenuHelper.findMenu(menuBar, "view"));
    }

    private void initialize(JTableHeader tableHeader, JMenu viewMenu) {
        viewMenu.addSeparator();
        JMenu columnMenu = JMenuHelper.createMenu("show-column");
        viewMenu.add(columnMenu);

        VisibleListener visibleListener = new VisibleListener();
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            column.addPropertyChangeListener(visibleListener);

            ToggleColumnVisibilityAction action = new ToggleColumnVisibilityAction(column);
            actionManager.register("show-column-" + column.getName(), action);

            String text = RouteConverter.getBundle().getString("show-column-prefix") + " " + RouteConverter.getBundle().getString(column.getName());
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
            menuItem.setModel(new PositionTableColumnButtonModel(column, action));

            popupMenu.add(menuItem);

            String menuBarText = RouteConverter.getBundle().getString(column.getName());
            JCheckBoxMenuItem menuBarItem = new JCheckBoxMenuItem(menuBarText);
            menuBarItem.setModel(new PositionTableColumnButtonModel(column, action));
            columnMenu.add(menuBarItem);
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

    private void showPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void visibilityChanged() {
        if (columnModel.getVisibleColumnCount() > 1)
            enableActions();
        else
            disableLastSelectedAction();
    }

    private List<ToggleColumnVisibilityAction> getActions() {
        List<ToggleColumnVisibilityAction> result = new ArrayList<ToggleColumnVisibilityAction>();
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            result.add((ToggleColumnVisibilityAction) actionManager.get("show-column-" + column.getName()));
        }
        return result;
    }

    private void enableActions() {
        for (Action action : getActions()) {
            if (!action.isEnabled())
                action.setEnabled(true);
        }
    }

    private void disableLastSelectedAction() {
        for (ToggleColumnVisibilityAction action : getActions()) {
            if (action.isSelected())
                action.setEnabled(false);
        }
    }

    private class VisibleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("visible"))
                visibilityChanged();
        }
    }

}
