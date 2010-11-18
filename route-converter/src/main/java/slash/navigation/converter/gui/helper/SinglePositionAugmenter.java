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

package slash.navigation.converter.gui.helper;

import slash.common.io.CompactCalendar;
import slash.navigation.completer.CompletePositionService;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.models.PositionColumns;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.util.Positions;

import javax.swing.*;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Helps to augment a newly created position with elevation, postal address
 * and time information.
 *
 * @author Christian Pesch
 */

public class SinglePositionAugmenter implements PositionAugmenter {
    private static final Logger log = Logger.getLogger(SinglePositionAugmenter.class.getName());
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private CompletePositionService completePositionService = new CompletePositionService();
    private PositionsModel positionsModel;

    public SinglePositionAugmenter(PositionsModel positionsModel) {
        this.positionsModel = positionsModel;
    }

    public void close() {
        completePositionService.dispose();
    }

    public void complementElevation(final int row, final Double longitude, final Double latitude) {
        executor.execute(new Runnable() {
            public void run() {
                final Integer[] elevation = new Integer[1];
                try {
                    elevation[0] = completePositionService.getElevationFor(longitude, latitude);
                } catch (IOException e) {
                    log.warning("Cannot retrieve elevation for " + longitude + "/" + latitude + ": " + e.getMessage());
                }

                if (elevation[0] != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (elevation[0] != null) {
                                positionsModel.edit(elevation[0], row, PositionColumns.ELEVATION_COLUMN_INDEX, true, false);
                            }
                        }
                    });
                }
            }
        });
    }

    public void complementComment(final int row, final Double longitude, final Double latitude) {
        executor.execute(new Runnable() {
            public void run() {
                final String[] comment = new String[1];
                try {
                    comment[0] = completePositionService.getCommentFor(longitude, latitude);
                } catch (IOException e) {
                    log.warning("Cannot retrieve comment for " + longitude + "/" + latitude + ": " + e.getMessage());
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

    public void complementTime(final int row, final CompactCalendar time) {
        if (time != null)
            return;

        executor.execute(new Runnable() {
            public void run() {
                final CompactCalendar time[] = new CompactCalendar[1];
                time[0] = row - 2 >= 0 ? Positions.interpolateTime(positionsModel.getPosition(row),
                        positionsModel.getPosition(row - 1), positionsModel.getPosition(row - 2)) : null;
                if (time[0] == null)
                    time[0] = CompactCalendar.fromCalendar(Calendar.getInstance());

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        positionsModel.edit(time[0], row, PositionColumns.TIME_COLUMN_INDEX, true, false);
                    }
                });
            }
        });
    }
}
