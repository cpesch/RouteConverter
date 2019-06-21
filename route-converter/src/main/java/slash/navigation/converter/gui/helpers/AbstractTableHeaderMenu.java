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
import slash.navigation.converter.gui.models.AbstractTableColumnModel;
import slash.navigation.converter.gui.models.PositionTableColumn;
import slash.navigation.converter.gui.models.PositionTableColumnButtonModel;
import slash.navigation.converter.gui.models.PositionsModel;
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

import static slash.navigation.converter.gui.models.PositionTableColumn.VISIBLE_PROPERTY_NAME;
import static slash.navigation.gui.helpers.JMenuHelper.setMnemonic;

/**
 * Helps to make popups for tables headers useable.
 *
 * @author Christian Pesch
 */

public abstract class AbstractTableHeaderMenu {
    private static final String SHOW_INFIX = "-show-";
    private static final String SORT_INFIX = "-sort-";
    private static final String MNEMONIC_SUFFIX = "-mnemonic";

    private final AbstractTableColumnModel columnModel;
    private final ActionManager actionManager;
    private final String preferencesPrefix;
    private final JPopupMenu popupMenu = new JPopupMenu();

    public AbstractTableHeaderMenu(AbstractTableColumnModel columnModel, ActionManager actionManager,
                                   String preferencesPrefix) {
        this.columnModel = columnModel;
        this.actionManager = actionManager;
        this.preferencesPrefix = preferencesPrefix;
    }

    private String createShowKey(String columnName) {
        return preferencesPrefix + SHOW_INFIX + columnName;
    }

    private String createSortKey(String columnName) {
        return preferencesPrefix + SORT_INFIX + columnName;
    }

    protected void initializeSortPositions(JMenu sortPositionListMenu, PositionsModel positionsModel) {
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            if (column.getComparator() == null)
                continue;

            String menuItemText = RouteConverter.getBundle().getString(column.getName());
            SortColumnAction action = new SortColumnAction(positionsModel, column);
            actionManager.register(createSortKey(column.getName()), action);

            JMenuItem menuBarItem = new JMenuItem(action);
            menuBarItem.setText(menuItemText);
            setMnemonic(menuBarItem, column.getName() + MNEMONIC_SUFFIX);
            sortPositionListMenu.add(menuBarItem);
        }
    }

    protected void initializeShowColumn(JMenu showColumnMenu) {
        VisibleListener visibleListener = new VisibleListener();
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            column.addPropertyChangeListener(visibleListener);

            String menuItemText = RouteConverter.getBundle().getString(column.getName());
            ToggleColumnVisibilityAction action = new ToggleColumnVisibilityAction(column);
            actionManager.register(createShowKey(column.getName()), action);

            JCheckBoxMenuItem popupItem = new JCheckBoxMenuItem(menuItemText);
            popupItem.setModel(new PositionTableColumnButtonModel(column, action));
            popupMenu.add(popupItem);

            if (showColumnMenu != null) {
                JCheckBoxMenuItem menuBarItem = new JCheckBoxMenuItem(menuItemText);
                menuBarItem.setModel(new PositionTableColumnButtonModel(column, action));
                setMnemonic(menuBarItem, column.getName() + MNEMONIC_SUFFIX);
                showColumnMenu.add(menuBarItem);
            }
        }
    }

    protected void initializePopup(JTableHeader tableHeader) {
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
        if (e.isPopupTrigger())
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private List<ToggleColumnVisibilityAction> getActions() {
        List<ToggleColumnVisibilityAction> result = new ArrayList<>();
        for (PositionTableColumn column : columnModel.getPreparedColumns()) {
            result.add((ToggleColumnVisibilityAction) actionManager.get(createShowKey(column.getName())));
        }
        return result;
    }

    private void enableAllActions() {
        for (ToggleColumnVisibilityAction action : getActions()) {
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

    public void enableSortActions(boolean enable) {
        for (PositionTableColumn column : columnModel.getPreparedColumns())
            if (column.getComparator() != null)
                actionManager.enable(createSortKey(column.getName()), enable);
    }

    private void visibilityChanged() {
        if (columnModel.getColumnCount() > 1)
            enableAllActions();
        else
            disableLastSelectedAction();
    }

    private class VisibleListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(VISIBLE_PROPERTY_NAME))
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

