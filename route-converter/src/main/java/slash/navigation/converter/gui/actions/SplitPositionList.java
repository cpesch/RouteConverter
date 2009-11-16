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

import slash.navigation.BaseNavigationFormat;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.BaseRoute;
import slash.navigation.NavigationFormat;
import slash.navigation.converter.gui.models.FormatAndRoutesModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.Constants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * {@link ActionListener} that splits the position list of a {@link PositionsModel} at
 * the selected rows of a {@link JTable} and adds them as separate position lists to a
 * {@link FormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

public class SplitPositionList implements ActionListener {
    private final JFrame frame;
    private final JTable table;
    private final PositionsModel positionsModel;
    private final FormatAndRoutesModel formatAndRoutesModel;

    public SplitPositionList(JFrame frame, JTable table, PositionsModel positionsModel, FormatAndRoutesModel formatAndRoutesModel) {
        this.frame = frame;
        this.table = table;
        this.positionsModel = positionsModel;
        this.formatAndRoutesModel = formatAndRoutesModel;
    }

    public void actionPerformed(ActionEvent e) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length > 0) {
            Constants.startWaitCursor(frame.getRootPane());

            try {
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
            } finally {
                Constants.stopWaitCursor(frame.getRootPane());
            }

            final int selectedRow = Math.max(selectedRows[selectedRows.length - 1] - 1, 0);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
                }
            });
        }
    }
}
