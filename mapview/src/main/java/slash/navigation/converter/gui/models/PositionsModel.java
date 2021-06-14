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

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.common.NavigationPosition;

import javax.swing.table.TableModel;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Acts as a {@link TableModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public interface PositionsModel extends TableModel {
    BaseRoute getRoute();
    void setRoute(BaseRoute route);

    NavigationPosition getPosition(int rowIndex);
    int getIndex(NavigationPosition position);
    List<NavigationPosition> getPositions(int[] rowIndices);
    List<NavigationPosition> getPositions(int firstIndex, int lastIndex);

    DistanceAndTimeAggregator getDistanceAndTimeAggregator();
    double[] getDistancesFromStart(int startIndex, int endIndex);
    double[] getDistancesFromStart(int[] indices);
    long[] getTimesFromStart(int startIndex, int endIndex);
    long[] getTimesFromStart(int[] indices);

    int[] getContainedPositions(BoundingBox boundingBox);
    int[] getPositionsWithinDistanceToPredecessor(double distance);
    int[] getInsignificantPositions(double threshold);
    int getClosestPosition(double longitude, double latitude, double threshold);
    int getClosestPosition(CompactCalendar time, long threshold);

    void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description);
    void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException;
    void add(int rowIndex, List<BaseNavigationPosition> positions);

    void edit(int rowIndex, PositionColumnValues columnToValues, boolean fireEvent, boolean trackUndo);

    void remove(int firstIndex, int lastIndex);
    void remove(int[] rowIndices);

    void sort(Comparator<NavigationPosition> comparator);
    void revert();

    void top(int[] rowIndices);
    void up(int[] rowIndices, int delta);
    void down(int[] rowIndices, int delta);
    void bottom(int[] rowIndices);

    boolean isContinousRange();
    void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex);
    void fireTableRowsUpdatedInContinousRange(int firstIndex, int lastIndex, int columnIndex);
}
