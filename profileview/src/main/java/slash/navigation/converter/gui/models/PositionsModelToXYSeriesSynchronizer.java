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

package slash.navigation.converter.gui.models;

import org.jfree.data.xy.XYSeries;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import static java.lang.Math.min;
import static javax.swing.event.TableModelEvent.*;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * Synchronizes changes at a {@link PositionsModel} with a {@link XYSeries}.
 *
 * @author Christian Pesch
 */

public abstract class PositionsModelToXYSeriesSynchronizer {
    private final PositionsModel positions;
    private final PatchedXYSeries series;

    protected PositionsModelToXYSeriesSynchronizer(PositionsModel positions, PatchedXYSeries series) {
        this.positions = positions;
        this.series = series;
    }

    protected PositionsModel getPositions() {
        return positions;
    }

    protected PatchedXYSeries getSeries() {
        return series;
    }

    protected void initialize() {
        handleAdd(0, positions.getRowCount() - 1);

        positions.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        handleAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        handleUpdate(e);
                        break;
                    case DELETE:
                         handleRemove(e.getFirstRow(), e.getLastRow());
                        break;
                    default:
                        throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
                }
            }
        });
    }

    protected void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            series.add(i - firstRow, i);
        }
    }

    private void handleUpdate(TableModelEvent e) {
        int columnIndex = e.getColumn();
        if (getPositions().isContinousRange()) {
            // handle distance and time column updates from the overlay position model - only once
            if (columnIndex == DISTANCE_COLUMN_INDEX) {
                handleFullUpdate();
            }
        } else if (isFirstToLastRow(e)) {
            // do a full update for routes to avoid IndexOutOfBoundsException from the depths of XYSeries
            handleFullUpdate();
        } else {
            int firstRow = e.getFirstRow();
            int lastRow = e.getLastRow();
            // ignored updates on columns not displayed
            if (columnIndex == LONGITUDE_COLUMN_INDEX ||
                    columnIndex == LATITUDE_COLUMN_INDEX ||
                    // don't do to avoid IndexOutOfBoundsException: columnIndex == DISTANCE_AND_TIME_COLUMN_INDEX ||
                    columnIndex == ALL_COLUMNS) {
                handleIntervalXUpdate(firstRow, lastRow);
            } else if (columnIndex == ELEVATION_COLUMN_INDEX ||
                    columnIndex == SPEED_COLUMN_INDEX) {
                handleIntervalYUpdate(firstRow, lastRow);
            }
        }
    }

    protected synchronized void handleFullUpdate() {
        series.delete(0, series.getItemCount() - 1);
        if (positions.getRowCount() > 0)
            handleAdd(0, positions.getRowCount() - 1);
    }

    protected synchronized void handleIntervalXUpdate(int firstRow, int lastRow) {
        getSeries().setFireSeriesChanged(false);
        series.delete(firstRow, min(lastRow, series.getItemCount() - 1));
        handleAdd(firstRow, lastRow);
        getSeries().setFireSeriesChanged(true);
        getSeries().fireSeriesChanged();
    }

    protected synchronized void handleIntervalYUpdate(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            series.update((double) i - firstRow, i);
        }
    }

    protected void handleRemove(int firstRow, int lastRow) {
        if (getPositions().isContinousRange())
            return;
        series.delete(firstRow, lastRow);
    }
}
