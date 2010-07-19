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

import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.JTableHelper;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.FrameAction;
import slash.navigation.util.Positions;

import javax.swing.*;
import java.util.Arrays;
import java.util.Calendar;

/**
 * {@link Action} that inserts a new {@link BaseNavigationPosition} after
 * the last selected row of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class NewPositionAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;

    public NewPositionAction(JTable table, PositionsModel positionsModel) {
        this.table = table;
        this.positionsModel = positionsModel;
    }

    private BaseNavigationPosition calculateCenter(int row) {
        BaseNavigationPosition position = positionsModel.getPosition(row);
        // if there is only one position or it is the first row, choose the map center
        if (row >= positionsModel.getRowCount() - 1)
            return null;
        // otherwise center between given positions
        BaseNavigationPosition second = positionsModel.getPosition(row + 1);
        if (!second.hasCoordinates() || !position.hasCoordinates())
            return null;
        return Positions.center(Arrays.asList(second, position));
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        int row = selectedRows.length > 0 ? selectedRows[0] : table.getRowCount();
        BaseNavigationPosition center = selectedRows.length > 0 ? calculateCenter(row) :
                positionsModel.getRowCount() > 0 ? calculateCenter(positionsModel.getRowCount() - 1) : null;
        final int insertRow = row > positionsModel.getRowCount() - 1 ? row : row + 1;

        RouteConverter r = RouteConverter.getInstance();
        if (center == null)
            center = r.getMapCenter();
        r.setLastMapCenter(center);

        positionsModel.add(insertRow, center.getLongitude(), center.getLatitude(),
                center.getElevation(), center.getSpeed(),
                center.getTime() != null ? center.getTime() : CompactCalendar.fromCalendar(Calendar.getInstance()),
                RouteConverter.getBundle().getString("new-position-comment"));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTableHelper.scrollToPosition(table, insertRow);
                JTableHelper.selectPositions(table, insertRow, insertRow);
            }
        });
    }
}