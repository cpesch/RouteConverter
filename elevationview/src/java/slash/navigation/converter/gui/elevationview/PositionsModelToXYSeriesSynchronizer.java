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

import slash.navigation.converter.gui.models.PositionsModel;
import org.jfree.data.xy.XYSeries;

import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

/**
 * Synchronizes changes at a {@link PositionsModel} with a {@link XYSeries}.
 *
 * @author Christian Pesch
 */

public class PositionsModelToXYSeriesSynchronizer {
    private PositionsModel positions;
    private XYSeries series;

    public PositionsModelToXYSeriesSynchronizer(PositionsModel positions, XYSeries series) {
        this.positions = positions;
        this.series = series;
        initialize();
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

    private void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            series.add(positions.getRoute().getDistance(0, i) / 1000.0, positions.getPosition(i).getElevation());
        }
    }

    private void handleUpdate(int firstRow, int lastRow) {
        // special treatment for fireTableDataChanged() notifications
        if (firstRow == 0 && lastRow == Integer.MAX_VALUE) {
            handleDelete(firstRow, series.getItemCount() - 1);
            if (positions.getRowCount() > 0)
                handleAdd(firstRow, positions.getRowCount() - 1);
        } else {
            handleDelete(firstRow, lastRow);
            handleAdd(firstRow, lastRow);
        }
    }

    private void handleDelete(int firstRow, int lastRow) {
        series.delete(firstRow, lastRow);
    }
}
