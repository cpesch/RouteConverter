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
import slash.navigation.completer.CompletePositionService;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionColumns;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.gui.FrameAction;
import slash.navigation.util.Positions;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * {@link Action} that inserts a new {@link BaseNavigationPosition} after
 * the last selected row of a {@link JTable}.
 *
 * @author Christian Pesch
 */

public class NewPositionAction extends FrameAction {
    private JTable table;
    private PositionsModel positionsModel;
    private PositionsSelectionModel positionsSelectionModel;

    public NewPositionAction(JTable table, PositionsModel positionsModel, PositionsSelectionModel positionsSelectionModel) {
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
        r.setLastMapCenter(center.getLongitude(), center.getLatitude());

        positionsModel.add(insertRow, center.getLongitude(), center.getLatitude(),
                center.getElevation(), center.getSpeed(),
                center.getTime() != null ? center.getTime() : CompactCalendar.fromCalendar(Calendar.getInstance()),
                MessageFormat.format(RouteConverter.getBundle().getString("new-position-name"), positionsModel.getRowCount() + 1));
        positionsSelectionModel.setSelectedPositions(new int[]{insertRow});

        complementComment(insertRow, center.getLongitude(), center.getLatitude());
        complementElevation(insertRow, center.getLongitude(), center.getLatitude());
    }

    protected static final Logger log = Logger.getLogger(NewPositionAction.class.getName());
    private ExecutorService executor = Executors.newCachedThreadPool();
    // TODO same as in BaseMapView
    private void complementElevation(final int row, final Double longitude, final Double latitude) {
        executor.execute(new Runnable() {
            public void run() {
                CompletePositionService completePositionService = new CompletePositionService();
                final Integer[] elevation = new Integer[1];
                try {
                    elevation[0] = completePositionService.getElevationFor(longitude, latitude);
                } catch (IOException e) {
                    log.warning("Cannot retrieve elevation for " + longitude + "/" + latitude + ": " + e.getMessage());
                } finally {
                    completePositionService.close();
                }

                if (elevation[0] != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (elevation[0] != null) {
                                RouteConverter r = RouteConverter.getInstance();
                                PositionsModel positionsModel = r.getPositionsModel();
                                positionsModel.edit(elevation[0], row, PositionColumns.ELEVATION_COLUMN_INDEX, true, false);
                            }
                        }
                    });
                }
            }
        });
    }
    private ExecutorService commentExecutor = Executors.newSingleThreadExecutor();
    // TODO same as in BaseMapView
    private void complementComment(final int row, final Double longitude, final Double latitude) {
        commentExecutor.execute(new Runnable() {
            public void run() {
                CompletePositionService completePositionService = new CompletePositionService();
                final String[] comment = new String[1];
                try {
                    comment[0] = completePositionService.getCommentFor(longitude, latitude);
                } catch (IOException e) {
                    log.warning("Cannot retrieve comment for " + longitude + "/" + latitude + ": " + e.getMessage());
                } finally {
                    completePositionService.close();
                }

                if (comment[0] != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (comment[0] != null)
                                positionsModel.edit(comment[0], row, PositionColumns.DESCRIPTION_COLUMN_INDEX, true, false);
                        }
                    });
                }
            }
        });
    }
}