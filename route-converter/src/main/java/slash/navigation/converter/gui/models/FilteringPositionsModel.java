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

import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.gui.models.FilterPredicate;
import slash.navigation.gui.models.FilteringTableModel;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;

import java.util.Comparator;
import java.util.List;

/**
 * Implements a {@link PositionsModel} that filters positions.
 *
 * @author Christian Pesch
 */

public class FilteringPositionsModel<P extends NavigationPosition> extends FilteringTableModel<P> implements PositionsModel {

    public FilteringPositionsModel(PositionsModel delegate, FilterPredicate<P> predicate) {
        super(delegate, predicate);
    }

    protected PositionsModel getDelegate() {
        return (PositionsModel) super.getDelegate();
    }

    public BaseRoute getRoute() {
        return getDelegate().getRoute();
    }

    public void setRoute(BaseRoute route) {
        getDelegate().setRoute(route);
    }

    public NavigationPosition getPosition(int rowIndex) {
        return getDelegate().getPosition(mapRow(rowIndex));
    }

    public int getIndex(NavigationPosition position) {
        return mapRow(getDelegate().getIndex(position));
    }

    public List<NavigationPosition> getPositions(int[] rowIndices) {
        throw new UnsupportedOperationException();
    }

    public List<NavigationPosition> getPositions(int firstIndex, int lastIndex) {
        throw new UnsupportedOperationException();
    }

    public DistanceAndTimeAggregator getDistanceAndTimeAggregator() {
        throw new UnsupportedOperationException();
    }

    public double[] getDistancesFromStart(int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }

    public double[] getDistancesFromStart(int[] indices) {
        throw new UnsupportedOperationException();
    }

    public long[] getTimesFromStart(int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }

    public long[] getTimesFromStart(int[] indices) {
        throw new UnsupportedOperationException();
    }

    public int[] getContainedPositions(BoundingBox boundingBox) {
        throw new UnsupportedOperationException();
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        throw new UnsupportedOperationException();
    }

    public int[] getInsignificantPositions(double threshold) {
        throw new UnsupportedOperationException();
    }

    public int getClosestPosition(double longitude, double latitude, double threshold) {
        throw new UnsupportedOperationException();
    }

    public int getClosestPosition(CompactCalendar time, long threshold) {
        throw new UnsupportedOperationException();
    }

    public void edit(int rowIndex, PositionColumnValues columnToValues, boolean fireEvent, boolean trackUndo) {
        getDelegate().edit(mapRow(rowIndex), columnToValues, fireEvent, trackUndo);
    }

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        throw new UnsupportedOperationException();
    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        throw new UnsupportedOperationException();
    }

    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        throw new UnsupportedOperationException();
    }

    public void remove(int firstIndex, int lastIndex) {
        throw new UnsupportedOperationException();
    }

    public void remove(int[] rowIndices) {
        getDelegate().remove(mapRows(rowIndices));
    }

    public void sort(Comparator<NavigationPosition> comparator) {
        throw new UnsupportedOperationException();
    }

    public void revert() {
        throw new UnsupportedOperationException();
    }

    public void top(int[] rowIndices) {
        throw new UnsupportedOperationException();
    }

    public void up(int[] rowIndices, int delta) {
        throw new UnsupportedOperationException();
    }

    public void down(int[] rowIndices, int delta) {
        throw new UnsupportedOperationException();
    }

    public void bottom(int[] rowIndices) {
        throw new UnsupportedOperationException();
    }

    public boolean isContinousRange() {
        return getDelegate().isContinousRange();
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        getDelegate().fireTableRowsUpdated(firstIndex, lastIndex, columnIndex);
    }

    public void fireTableRowsUpdatedInContinousRange(int firstIndex, int lastIndex, int columnIndex) {
        getDelegate().fireTableRowsUpdatedInContinousRange(firstIndex, lastIndex, columnIndex);
    }
}
