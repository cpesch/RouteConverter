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

import slash.navigation.*;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * {@link ActionListener} that splits the position list of a {@link PositionsModel} at
 * the selected rows of a {@link JTable} and adds them as separate position lists to a
 * {@link FormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

public class SplitPositionList extends AbstractAction {
    private JTable table;
    private JComboBox combobox;
    private PositionsModel positionsModel;
    private FormatAndRoutesModel formatAndRoutesModel;

    public SplitPositionList(JTable table, JComboBox combobox, PositionsModel positionsModel, FormatAndRoutesModel formatAndRoutesModel) {
        this.table = table;
        this.combobox = combobox;
        this.positionsModel = positionsModel;
        this.formatAndRoutesModel = formatAndRoutesModel;
        initialize();
    }

    protected void initialize() {
        /*
        boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
        boolean existsARoute = getFormatAndRoutesModel().getSize() > 0;
        boolean existsOneRoute = getFormatAndRoutesModel().getSize() == 1;
        boolean existsMoreThanOneRoute = getFormatAndRoutesModel().getSize() > 1;
        */

        // private void handleFormatUpdate() {
        // TODO check this later buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsMoreThanOnePosition);

        // private void handleRoutesUpdate() {
        // TODO check this later buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsARoute);

        // private void handlePositionsUpdate() {
        // TODO check this later buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsMoreThanOnePosition);

        /*
        combobox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean supportsMultipleRoutes = getFormat() instanceof MultipleRoutesFormat;
                boolean existsMoreThanOnePosition = positionsModel.getRowCount() > 1;
                buttonSplitPositionList.setEnabled(supportsMultipleRoutes && existsMoreThanOnePosition);
            }
        });
        */
    }

    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            BaseRoute selectedRoute = formatAndRoutesModel.getSelectedRoute();
            int routeInsertIndex = formatAndRoutesModel.getSize();

            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int fromIndex = selectedRows[i] - 1;
                fromIndex = Math.max(fromIndex, 0);
                int toIndex = i + 1 < selectedRows.length ? selectedRows[i + 1] : positionsModel.getRowCount();
                toIndex--;
                toIndex = Math.max(toIndex, 0);
                if (fromIndex == 0 && toIndex == 0)
                    break;

                List<BaseNavigationPosition> positions = positionsModel.remove(fromIndex, toIndex);
                NavigationFormat format = formatAndRoutesModel.getFormat();
                @SuppressWarnings({"unchecked"})
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> target = 
                        format.createRoute(selectedRoute.getCharacteristics(), selectedRoute.getName() + "(" + (i + 1) + ")", positions);
                formatAndRoutesModel.addRoute(routeInsertIndex, target);
            }
        }
    }
}
