/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.actions;

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.helpers.PositionAugmenter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.gui.actions.FrameAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.common.BoundingBox.asBoundingBox;
import static slash.navigation.gui.events.Range.revert;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;

/**
 * {@link Action} that inserts a new {@link BaseNavigationPosition} after
 * the last selected row of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class AddPositionAction extends FrameAction {
    private final JTable table;
    private final PositionsModel positionsModel;
    private final PositionsSelectionModel positionsSelectionModel;

    public AddPositionAction(JTable table, PositionsModel positionsModel, PositionsSelectionModel positionsSelectionModel) {
        this.table = table;
        this.positionsModel = positionsModel;
        this.positionsSelectionModel = positionsSelectionModel;
    }

    /**
     * The center of the segment starting at {@code row}, i.e. between {@code row} and {@code row + 1}.
     * Returns {@code null} only when there is no such segment (row is out of range or the last row) or
     * a coordinate is missing, in which case the caller falls back to the map center.
     */
    private NavigationPosition calculateCenter(int row) {
        if (row < 0 || row >= positionsModel.getRowCount() - 1)
            return null;
        NavigationPosition position = positionsModel.getPosition(row);
        NavigationPosition second = positionsModel.getPosition(row + 1);
        if (!second.hasCoordinates() || !position.hasCoordinates())
            return null;
        return asBoundingBox(asList(second, position)).getCenter();
    }

    /**
     * The row whose following segment the "+" bisects, given a selected {@code row}: the selected row
     * itself, or - when it is the last position - the segment before it, so the inserted position always
     * lands on the route. Returns -1 when the route has fewer than two positions (nothing to bisect, the
     * caller seeds at the map center). The result is always a valid segment start in [0, rowCount - 2].
     */
    static int segmentRowToBisect(int row, int rowCount) {
        if (rowCount < 2)
            return -1;
        return row >= rowCount - 1 ? rowCount - 2 : row;
    }

    private PositionAugmenter getBatchPositionAugmenter() {
        return BaseRouteConverter.getInstance().getPositionAugmenter();
    }

    private NavigationPosition insertPosition(int row, NavigationPosition position) {
        String description = getBatchPositionAugmenter().createDescription(positionsModel.getRowCount() + 1, null);
        positionsModel.add(row, position.getLongitude(), position.getLatitude(), position.getElevation(),
                position.getSpeed(), position.getTime(), description);
        return positionsModel.getPosition(row);
    }

    public void run() {
        List<NavigationPosition> insertedPositions = new ArrayList<>();
        int[] rowIndices = revert(table.getSelectedRows());
        int rowCount = positionsModel.getRowCount();
        // nothing selected: behave as if the last position were selected, so the new position is inserted
        // into the last segment of the route rather than dumped at the (view-dependent) map center
        if (rowIndices.length == 0)
            rowIndices = new int[]{rowCount - 1};
        boolean hasInsertedRowInMapCenter = false;
        for (int row : rowIndices) {
            int segmentRow = segmentRowToBisect(row, rowCount);
            NavigationPosition center = segmentRow < 0 ? null : calculateCenter(segmentRow);
            int insertRow;
            if (center == null) {
                // no segment to bisect (empty list or a single position): seed at the map center once
                if (hasInsertedRowInMapCenter)
                    continue;
                center = BaseRouteConverter.getInstance().getMapCenter();
                hasInsertedRowInMapCenter = true;
                insertRow = Math.min(row + 1, rowCount);
            } else {
                insertRow = segmentRow + 1;
            }

            insertedPositions.add(insertPosition(insertRow, center));
        }

        if (!insertedPositions.isEmpty()) {
            final int[] rows = insertedPositions.stream()
                    .mapToInt(positionsModel::getIndex)
                    .toArray();
            getBatchPositionAugmenter().addData(rows, true, true, true, true, false);
            final int insertRow = rows.length > 0 ? rows[0] : table.getRowCount();
            invokeLater(() -> {
                scrollToPosition(table, insertRow);
                positionsSelectionModel.setSelectedPositions(rows, true);
            });
        }
    }
}
