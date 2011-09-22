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
import slash.navigation.base.BaseRoute;
import slash.navigation.converter.gui.models.PositionsModel;

/**
 * Provides a {@link XYSeries} model by extracting the elevation from a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class ElevationModel extends PositionsModelToXYSeriesSynchronizer {
    public ElevationModel(PositionsModel positions, PatchedXYSeries series) {
        super(positions, series);
    }

    protected void handleAdd(int firstRow, int lastRow) {
        recomputeEverythingAfter(firstRow);
    }

    protected void handleFullUpdate() {
        recomputeEverythingAfter(0);
    }

    protected void handleIntervalXUpdate(int firstRow, int lastRow) {
        recomputeEverythingAfter(firstRow);
    }

    protected void handleIntervalYUpdate(int firstRow, int lastRow) {
        getSeries().setFireSeriesChanged(false);
        for (int i = firstRow; i < lastRow + 1; i++) {
            getSeries().updateByIndex(i, getPositions().getPosition(i).getElevation());
        }
        getSeries().setFireSeriesChanged(true);
        getSeries().fireSeriesChanged();
    }

    protected void handleDelete(int firstRow, int lastRow) {
        recomputeEverythingAfter(firstRow);
    }

    private void recomputeEverythingAfter(int firstRow) {
        getSeries().setFireSeriesChanged(false);

        int itemCount = getSeries().getItemCount();
        if (itemCount > 0 && firstRow < itemCount - 1)
            getSeries().delete(firstRow, itemCount - 1);

        BaseRoute route = getPositions().getRoute();
        if(route == null)
            return;

        int lastRow = getPositions().getRowCount() - 1;
        if (firstRow <= lastRow && lastRow >= 0) {
            double[] distances = route.getDistancesFromStart(firstRow, lastRow);
            for (int i = firstRow; i < lastRow + 1; i++) {
                getSeries().add(distances[i - firstRow] / 1000.0, getPositions().getPosition(i).getElevation(), false);
            }
        }

        getSeries().setFireSeriesChanged(true);
        getSeries().fireSeriesChanged();
    }
}
