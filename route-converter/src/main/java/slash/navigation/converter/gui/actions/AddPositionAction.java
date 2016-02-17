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
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.PositionAugmenter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Transfer.toArray;
import static slash.navigation.gui.events.Range.revert;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;

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

    private NavigationPosition calculateCenter(int row) {
        NavigationPosition position = positionsModel.getPosition(row);
        // if there is only one position or it is the first row, choose the map center
        if (row >= positionsModel.getRowCount() - 1)
            return null;
        // otherwise center between given positions
        NavigationPosition second = positionsModel.getPosition(row + 1);
        if (!second.hasCoordinates() || !position.hasCoordinates())
            return null;
        return new BoundingBox(asList(second, position)).getCenter();
    }

    private PositionAugmenter getBatchPositionAugmenter() {
        return RouteConverter.getInstance().getPositionAugmenter();
    }

    private NavigationPosition insertRow(int row, NavigationPosition position) {
        String description = getBatchPositionAugmenter().createDescription(positionsModel.getRowCount() + 1, null);
        positionsModel.add(row, position.getLongitude(), position.getLatitude(), position.getElevation(),
                position.getSpeed(), position.getTime(), description);
        return positionsModel.getPosition(row);
    }

    private void complementRow(int row) {
        getBatchPositionAugmenter().addData(new int[]{row}, true, true, true, true, false);
    }

    public void run() {
        boolean hasInsertedRowInMapCenter = false;
        List<NavigationPosition> insertedPositions = new ArrayList<>();
        int[] rowIndices = revert(table.getSelectedRows());
        // append to table if there is nothing selected
        boolean areRowsSelected = rowIndices.length > 0;
        if (!areRowsSelected)
            rowIndices = new int[]{table.getRowCount()};
        for (int row : rowIndices) {
            int insertRow = row > positionsModel.getRowCount() - 1 ? row : row + 1;

            NavigationPosition center = areRowsSelected ? calculateCenter(row) :
                    positionsModel.getRowCount() > 0 ? calculateCenter(positionsModel.getRowCount() - 1) : null;
            if (center == null) {
                // only insert row in map center once
                if (hasInsertedRowInMapCenter)
                    continue;
                center = RouteConverter.getInstance().getMapCenter();
                hasInsertedRowInMapCenter = true;
            }

            insertedPositions.add(insertRow(insertRow, center));
        }

        if (insertedPositions.size() > 0) {
            List<Integer> insertedRows = new ArrayList<>();
            for (NavigationPosition position : insertedPositions) {
                int index = positionsModel.getIndex(position);
                insertedRows.add(index);
                complementRow(index);
            }

            final int[] rows = toArray(insertedRows);
            final int insertRow = rows.length > 0 ? rows[0] : table.getRowCount();
            invokeLater(new Runnable() {
                public void run() {
                    scrollToPosition(table, insertRow);
                    positionsSelectionModel.setSelectedPositions(rows, true);
                }
            });
        }
    }
}