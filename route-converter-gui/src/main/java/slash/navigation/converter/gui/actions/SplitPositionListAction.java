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

import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;

import static java.lang.Math.max;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.base.RouteComments.getRouteName;

/**
 * {@link ActionListener} that splits the position list of a {@link PositionsModel} at
 * the selected rows of a {@link JTable} and adds them as separate position lists to a
 * {@link FormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

public class SplitPositionListAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;
    private final FormatAndRoutesModel formatAndRoutesModel;

    public SplitPositionListAction(JTable table, PositionsModel positionsModel, FormatAndRoutesModel formatAndRoutesModel) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.formatAndRoutesModel = formatAndRoutesModel;
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            BaseRoute selectedRoute = formatAndRoutesModel.getSelectedRoute();
            int routeInsertIndex = formatAndRoutesModel.getIndex(selectedRoute) + 1;

            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int fromIndex = selectedRows[i];
                fromIndex = max(fromIndex, 0);
                int toIndex = i + 1 < selectedRows.length ? selectedRows[i + 1] : positionsModel.getRowCount();
                toIndex = max(toIndex, 0);
                if (fromIndex == 0 && toIndex == 0)
                    break;

                List<NavigationPosition> positions = positionsModel.getPositions(fromIndex, toIndex);
                positionsModel.remove(fromIndex, toIndex);
                NavigationFormat format = formatAndRoutesModel.getFormat();
                @SuppressWarnings({"unchecked"})
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> target =
                        format.createRoute(selectedRoute.getCharacteristics(), getRouteName(selectedRoute, routeInsertIndex), positions);
                formatAndRoutesModel.addPositionList(routeInsertIndex, target);
            }

            final int selectedRow = max(selectedRows[selectedRows.length - 1] - 1, 0);
            invokeLater(new Runnable() {
                public void run() {
                    table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
                }
            });
        }
    }
}
