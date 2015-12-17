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
import slash.navigation.base.BaseRoute;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.profileview.XAxisMode;
import slash.navigation.converter.gui.profileview.YAxisMode;

import static java.lang.String.format;
import static slash.navigation.common.UnitConversion.METERS_OF_A_KILOMETER;
import static slash.navigation.converter.gui.profileview.XAxisMode.Distance;

/**
 * Provides a {@link XYSeries} model by extracting profile information from a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class ProfileModel extends PositionsModelToXYSeriesSynchronizer {
    private UnitSystem unitSystem;
    private XAxisMode xAxisMode;
    private YAxisMode yAxisMode;

    public ProfileModel(PositionsModel positions, PatchedXYSeries series, UnitSystem unitSystem, XAxisMode xAxisMode, YAxisMode yAxisMode) {
        super(positions, series);
        this.unitSystem = unitSystem;
        this.xAxisMode = xAxisMode;
        this.yAxisMode = yAxisMode;
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
            getSeries().updateByIndex(i, formatYValue(getPositions().getPosition(i)));
        }
        getSeries().setFireSeriesChanged(true);
        getSeries().fireSeriesChanged();
    }

    protected void handleRemove(int firstRow, int lastRow) {
        recomputeEverythingAfter(firstRow);
    }

    private void recomputeEverythingAfter(int firstRow) {
        getSeries().setFireSeriesChanged(false);

        int itemCount = getSeries().getItemCount();
        if (itemCount > 0 && firstRow < itemCount - 1)
            getSeries().delete(firstRow, itemCount - 1);

        BaseRoute route = getPositions().getRoute();
        if (route == null)
            return;

        int lastRow = getPositions().getRowCount() - 1;
        if (firstRow <= lastRow && lastRow >= 0) {
            if(getXAxisMode().equals(Distance)) {
                double[] distances = route.getDistancesFromStart(firstRow, lastRow);
                for (int i = firstRow; i < lastRow + 1; i++) {
                    getSeries().add(formatDistance(distances[i - firstRow]), formatYValue(getPositions().getPosition(i)), false);
                }
            } else {
                long[] times = route.getTimesFromStart(firstRow, lastRow);
                for (int i = firstRow; i < lastRow + 1; i++) {
                    getSeries().add(formatTime(times[i - firstRow]), formatYValue(getPositions().getPosition(i)), false);
                }
            }
        }

        getSeries().setFireSeriesChanged(true);
        getSeries().fireSeriesChanged();
    }

    private Double formatYValue(NavigationPosition position) {
        switch(yAxisMode) {
            case Elevation:
                return formatElevation(position.getElevation());
            case Speed:
                return formatSpeed(position.getSpeed());
            default:
                throw new IllegalArgumentException(format("X-Axis mode %s is not supported", yAxisMode));
        }
    }

    public double formatDistance(double distance) {
        return unitSystem.distanceToUnit(distance / METERS_OF_A_KILOMETER);
    }

    public long formatTime(long time) {
        return time / 1000;
    }

    private Double formatElevation(Double elevation) {
        return unitSystem.valueToUnit(elevation);
    }

    private Double formatSpeed(Double speed) {
        return unitSystem.distanceToUnit(speed);
    }

    public UnitSystem getUnitSystem() {
        return unitSystem;
    }

    public void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
        handleFullUpdate();
    }

    public XAxisMode getXAxisMode() {
        return xAxisMode;
    }

    public YAxisMode getYAxisMode() {
        return yAxisMode;
    }

    public void setProfileMode(XAxisMode xAxisMode, YAxisMode yAxisMode) {
        this.xAxisMode = xAxisMode;
        this.yAxisMode = yAxisMode;
        handleFullUpdate();
    }
}
