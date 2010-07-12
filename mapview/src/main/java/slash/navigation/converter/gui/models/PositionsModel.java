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

import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;

import javax.swing.table.TableModel;
import java.io.IOException;
import java.util.List;

/**
 * Acts as a {@link TableModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public interface PositionsModel extends TableModel {
    BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getRoute();
    void setRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route);

    BaseNavigationPosition getPredecessor(BaseNavigationPosition position);
    BaseNavigationPosition getPosition(int rowIndex);
    int getIndex(BaseNavigationPosition position);
    List<BaseNavigationPosition> getPositions(int[] rowIndices);
    List<BaseNavigationPosition> getPositions(int from, int to);

    int[] getPositionsWithinDistanceToPredecessor(double distance);
    int[] getInsignificantPositions(double threshold);

    void add(int row, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment);
    void add(int row, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException;
    void add(int row, List<BaseNavigationPosition> positions);

    void remove(int from, int to);
    void remove(int[] rows);

    void revert();

    void top(int[] rows);
    void up(int[] rows);
    void down(int[] rows);
    void bottom(int[] rows);

    void fireTableRowsUpdated(int from, int to);
}
