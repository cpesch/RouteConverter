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

package slash.navigation.converter.gui.elevationview;

import org.jfree.data.xy.XYSeries;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Synchronizes changes at a {@link PositionsModel} with a {@link XYSeries}.
 *
 * @author Christian Pesch
 */

public abstract class PositionsModelToXYSeriesSynchronizer {
    private PositionsModel positions;
    private XYSeries series;

    protected PositionsModelToXYSeriesSynchronizer(PositionsModel positions, XYSeries series) {
        this.positions = positions;
        this.series = series;
        initialize();
    }

    protected PositionsModel getPositions() {
        return positions;
    }

    protected XYSeries getSeries() {
        return series;
    }

    private void initialize() {
        handleAdd(0, positions.getRowCount() - 1);

        positions.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case TableModelEvent.INSERT:
                        handleAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case TableModelEvent.UPDATE:
                        handleUpdate(e.getFirstRow(), e.getLastRow());
                        break;
                    case TableModelEvent.DELETE:
                        handleDelete(e.getFirstRow(), e.getLastRow());
                        break;
                }
            }
        });
    }

    protected void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            series.add(i - firstRow, i);
        }
    }

    private void handleUpdate(int firstRow, int lastRow) {
        // special treatment for fireTableDataChanged() notifications
        if (firstRow == 0 && lastRow == Integer.MAX_VALUE ||
                // since PositionsModel#revert fires fireTableRowsUpdated(-1, -1) for a complete update
                firstRow == -1 && lastRow == -1) {
            handleFullUpdate();
        } else {
            handleIntervalUpdate(firstRow, lastRow);
        }
    }

    private void handleFullUpdate() {
        handleDelete(0, series.getItemCount() - 1);
        if (positions.getRowCount() > 0)
            handleAdd(0, positions.getRowCount() - 1);
    }

    protected void handleIntervalUpdate(int firstRow, int lastRow) {
        handleDelete(firstRow, lastRow);
        handleAdd(firstRow, lastRow);
    }

    private void handleDelete(int firstRow, int lastRow) {
        series.delete(firstRow, lastRow);
    }
}
