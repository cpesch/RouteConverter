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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static javax.swing.event.TableModelEvent.*;
import static slash.navigation.converter.gui.models.PositionColumns.*;

/**
 * Synchronizes changes at a {@link PositionsModel} with a {@link XYSeries}.
 *
 * @author Christian Pesch
 */

public abstract class PositionsModelToXYSeriesSynchronizer {
    private PositionsModel positions;
    private PatchedXYSeries series;

    protected PositionsModelToXYSeriesSynchronizer(PositionsModel positions, PatchedXYSeries series) {
        this.positions = positions;
        this.series = series;
        initialize();
    }

    protected PositionsModel getPositions() {
        return positions;
    }

    protected PatchedXYSeries getSeries() {
        return series;
    }

    private void initialize() {
        handleAdd(0, positions.getRowCount() - 1);

        positions.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        handleAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        handleUpdate(e.getFirstRow(), e.getLastRow(), e.getColumn());
                        break;
                    case DELETE:
                        if (getPositions().isContinousRange())
                            return;
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

    private void handleUpdate(int firstRow, int lastRow, int columnIndex) {
        // special treatment for fireTableDataChanged() notifications
        if (firstRow == 0 && lastRow == MAX_VALUE ||
                // since PositionsModel#revert fires fireTableRowsUpdated(-1, -1) for a complete update
                firstRow == -1 && lastRow == -1) {
            handleFullUpdate();
        } else {
            // ignored updates on columns not displayed
            if (columnIndex == LONGITUDE_COLUMN_INDEX ||
                    columnIndex == LATITUDE_COLUMN_INDEX ||
                    columnIndex == ALL_COLUMNS) {
                handleIntervalXUpdate(firstRow, lastRow);
            } else if (columnIndex == ELEVATION_COLUMN_INDEX ||
                    columnIndex == SPEED_COLUMN_INDEX) {
                handleIntervalYUpdate(firstRow, lastRow);
            }
        }
    }

    protected void handleFullUpdate() {
        series.delete(0, series.getItemCount() - 1);
        if (positions.getRowCount() > 0)
            handleAdd(0, positions.getRowCount() - 1);
    }

    protected void handleIntervalXUpdate(int firstRow, int lastRow) {
        getSeries().setFireSeriesChanged(false);
        series.delete(firstRow, min(lastRow, series.getItemCount() - 1));
        handleAdd(firstRow, lastRow);
        getSeries().setFireSeriesChanged(true);
        getSeries().fireSeriesChanged();
    }

    protected void handleIntervalYUpdate(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            series.update((double) i - firstRow, i);
        }
    }

    protected void handleRemove(int firstRow, int lastRow) {
        series.delete(firstRow, lastRow);
    }
}
