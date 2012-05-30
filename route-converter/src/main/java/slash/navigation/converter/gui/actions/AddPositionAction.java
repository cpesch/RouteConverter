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

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.util.NumberPattern;

import javax.swing.*;

import static java.util.Arrays.asList;
import static slash.navigation.util.Positions.center;
import static slash.navigation.util.RouteComments.formatNumberedPosition;

/**
 * {@link Action} that inserts a new {@link BaseNavigationPosition} after
 * the last selected row of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class AddPositionAction extends FrameAction {
    private JTable table;
    private PositionsModel positionsModel;
    private PositionsSelectionModel positionsSelectionModel;

    public AddPositionAction(JTable table, PositionsModel positionsModel, PositionsSelectionModel positionsSelectionModel) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.positionsSelectionModel = positionsSelectionModel;
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
        return center(asList(second, position));
    }

    private String getRouteComment() {
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        String number = Integer.toString(positionsModel.getRowCount() + 1);
        String description = RouteConverter.getBundle().getString("new-position-name");
        return formatNumberedPosition(numberPattern, number, description);
    }

    public void run() {
        int[] selectedRows = table.getSelectedRows();
        int row = selectedRows.length > 0 ? selectedRows[0] : table.getRowCount();
        BaseNavigationPosition center = selectedRows.length > 0 ? calculateCenter(row) :
                positionsModel.getRowCount() > 0 ? calculateCenter(positionsModel.getRowCount() - 1) : null;
        int insertRow = row > positionsModel.getRowCount() - 1 ? row : row + 1;

        RouteConverter r = RouteConverter.getInstance();
        if (center == null)
            center = r.getMapCenter();
        r.setLastMapCenter(center.getLongitude(), center.getLatitude());

        positionsModel.add(insertRow, center.getLongitude(), center.getLatitude(), center.getElevation(),
                center.getSpeed(), center.getTime(), getRouteComment());
        positionsSelectionModel.setSelectedPositions(new int[]{insertRow}, true);

        r.complementComment(insertRow, center.getLongitude(), center.getLatitude());
        r.complementElevation(insertRow, center.getLongitude(), center.getLatitude());
        r.complementTime(insertRow, center.getTime());
    }
}