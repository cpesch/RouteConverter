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
import slash.navigation.base.ExtendedSensorNavigationPosition;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.profileview.XAxisMode;
import slash.navigation.converter.gui.profileview.YAxisMode;

import static java.lang.Math.min;
import static java.lang.String.format;
import static slash.common.io.Transfer.isEmpty;
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
        initialize();
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
        if(getSeries().getItemCount() > 0) {
            getSeries().setFireSeriesChanged(false);

            for (int i = firstRow; i < lastRow + 1; i++) {
                getSeries().updateByIndex(i, formatYValue(getPositions().getPosition(i)));
            }
            getSeries().setFireSeriesChanged(true);
        }
        getSeries().fireSeriesChanged();
    }

    protected void handleRemove(int firstRow, int lastRow) {
        recomputeEverythingAfter(firstRow);
    }

    private synchronized void recomputeEverythingAfter(int firstRow) {
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
                double[] distances = getPositions().getDistancesFromStart(firstRow, lastRow);
                if(distances != null) {
                    for (int i = firstRow; i < lastRow + 1; i++) {
                        Double distance = formatDistance(distances[i - firstRow]);
                        // don't use isEmpty() since the first distance is always zero
                        if (distance != null)
                            getSeries().add(distance, formatYValue(getPositions().getPosition(i)), false);
                    }
                }
            } else {
                long[] times = getPositions().getTimesFromStart(firstRow, lastRow);
                if(times != null) {
                    for (int i = firstRow; i < lastRow + 1; i++) {
                        // XYSeries only works with doubles so it's hard to format the time as a date and time string
                        getSeries().add(formatTime(times[i - firstRow]), formatYValue(getPositions().getPosition(i)), false);
                    }
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
            case HeartBeat:
                return formatHeartBeat(position);
            default:
                throw new IllegalArgumentException(format("X-Axis mode %s is not supported", yAxisMode));
        }
    }

    public Double formatDistance(Double distance) {
        return unitSystem.distanceToUnit(distance);
    }

    public long formatTime(long timeInMilliseconds) {
        return timeInMilliseconds / 1000;
    }

    private Double formatElevation(Double elevation) {
        return unitSystem.valueToUnit(elevation);
    }

    private Double formatSpeed(Double speed) {
        return speed != null ? unitSystem.distanceToUnit(speed * METERS_OF_A_KILOMETER) : null;
    }

    private Double formatHeartBeat(NavigationPosition position) {
        Short heartBeat = null;
        if (position instanceof ExtendedSensorNavigationPosition)
            heartBeat = ((ExtendedSensorNavigationPosition) position).getHeartBeat();
        return heartBeat != null ? heartBeat.doubleValue() : null;
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
