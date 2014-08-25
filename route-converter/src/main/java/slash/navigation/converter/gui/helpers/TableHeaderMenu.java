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

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.actions.ToggleColumnVisibilityAction;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionTableColumnButtonModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsTableColumnModel;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static slash.navigation.gui.helpers.JMenuHelper.findMenu;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Creates a {@link JMenu} and a {@link JPopupMenu} for a {@link PositionsTableColumnModel}.
 *
 * @author Christian Pesch
 */

public class TableHeaderMenu {
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final PositionsModel positionsModel;
    private final PositionsTableColumnModel columnModel;
    private final ActionManager actionManager;

    public TableHeaderMenu(JTableHeader tableHeader, JMenuBar menuBar, PositionsModel positionsModel,
                           PositionsTableColumnModel columnModel, ActionManager actionManager) {
        this.positionsModel = positionsModel;
        this.columnModel = columnModel;
        this.actionManager = actionManager;

        initializeShowColumn(findMenu(menuBar, "view", "show-column"));
        initializeSortPositions(findMenu(menuBar, "positionlist", "sort-positions"));

        tableHeader.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }
        });
    }

    private void initializeSortPositions(JMenu sortPositionListMenu) {
        // JMenu sortPositionsPopupMenu = createMenu("sort-positions");
        // popupMenu.add(sortPositionsPopupMenu);

        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            if(column.getComparator() == null)
                continue;

            String menuItemText = RouteConverter.getBundle().getString(column.getName());
            SortColumnAction action = new SortColumnAction(positionsModel, column);
            actionManager.register("sort-column-" + column.getName(), action);

            // JMenuItem popupItem = new JMenuItem(action);
            // popupItem.setText(menuItemText);
            // sortPositionsPopupMenu.add(popupItem);

            JMenuItem menuBarItem = new JMenuItem(action);
            menuBarItem.setText(menuItemText);
            setMnemonic(menuBarItem, column.getName() + "-mnemonic");
            sortPositionListMenu.add(menuBarItem);
        }
    }

    private void initializeShowColumn(JMenu showColumnMenu) {
        // JMenu showColumnPopupMenu = createMenu("show-column-popup");
        // popupMenu.add(showColumnPopupMenu);

        VisibleListener visibleListener = new VisibleListener();
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            column.addPropertyChangeListener(visibleListener);

            String menuItemText = RouteConverter.getBundle().getString(column.getName());
            ToggleColumnVisibilityAction action = new ToggleColumnVisibilityAction(column);
            actionManager.register("show-column-" + column.getName(), action);

            JCheckBoxMenuItem popupItem = new JCheckBoxMenuItem(menuItemText);
            popupItem.setModel(new PositionTableColumnButtonModel(column, action));
            popupMenu.add(popupItem);

            JCheckBoxMenuItem menuBarItem = new JCheckBoxMenuItem(menuItemText);
            menuBarItem.setModel(new PositionTableColumnButtonModel(column, action));
            setMnemonic(menuBarItem, column.getName() + "-mnemonic");
            showColumnMenu.add(menuBarItem);
        }
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
        List<ToggleColumnVisibilityAction> result = new ArrayList<>();
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

    public void enable(boolean enable) {
        for (PositionTableColumn column : columnModel.getPreparedColumns())
            if (column.getComparator() != null)
                actionManager.enable("sort-column-" + column.getName(), enable);
    }

    private class VisibleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("visible"))
                visibilityChanged();
        }
    }

    private static class SortColumnAction extends FrameAction {
        private final PositionsModel positionsModel;
        private final PositionTableColumn column;

        public SortColumnAction(PositionsModel positionsModel, PositionTableColumn column) {
            this.positionsModel = positionsModel;
            this.column = column;
        }

        public void run() {
            positionsModel.sort(column.getComparator());
        }
    }
}
