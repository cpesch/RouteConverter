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
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.Range;
import slash.navigation.gui.events.RangeOperation;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Collections.singletonList;
import static javax.swing.event.TableModelEvent.*;
import static slash.navigation.base.NavigationFormatConverter.convertPositions;
import static slash.navigation.converter.gui.models.PositionColumns.*;

/**
 * Implements the {@link PositionsModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsModelImpl extends AbstractTableModel implements PositionsModel {
    private static final Logger log = Logger.getLogger(PositionsModelImpl.class.getName());

    private final PositionsModelCallback positionsModelCallback;

    public PositionsModelImpl(PositionsModelCallback positionsModelCallback) {
        this.positionsModelCallback = positionsModelCallback;
    }

    private BaseRoute route;

    public BaseRoute getRoute() {
        return route;
    }

    public void setRoute(BaseRoute route) {
        this.route = route;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return getRoute() != null ? getRoute().getPositionCount() : 0;
    }

    public int getColumnCount() {
        throw new IllegalArgumentException("This is determined by the PositionsTableColumnModel");
    }

    public/*for UndoPositionsModel*/ String getStringAt(int rowIndex, int columnIndex) {
        return positionsModelCallback.getStringAt(getPosition(rowIndex), columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return getPosition(rowIndex);
    }

    public NavigationPosition getPosition(int rowIndex) {
        return getRoute().getPosition(rowIndex);
    }

    @SuppressWarnings({"unchecked"})
    public int getIndex(NavigationPosition position) {
        return getRoute().getIndex((BaseNavigationPosition) position);
    }

    public List<NavigationPosition> getPositions(int[] rowIndices) {
        List<NavigationPosition> result = new ArrayList<>(rowIndices.length);
        for (int rowIndex : rowIndices)
            result.add(getPosition(rowIndex));
        return result;
    }

    public List<NavigationPosition> getPositions(int firstIndex, int lastIndex) {
        List<NavigationPosition> result = new ArrayList<>(lastIndex - firstIndex);
        for (int i = firstIndex; i < lastIndex; i++)
            result.add(getPosition(i));
        return result;
    }

    public DistanceAndTimeAggregator getDistanceAndTimeAggregator() {
      throw new UnsupportedOperationException();
    }

    public double[] getDistancesFromStart(int startIndex, int endIndex) {
        return getRoute().getDistancesFromStart(startIndex, endIndex);
    }

    public double[] getDistancesFromStart(int[] indices) {
        return getRoute().getDistancesFromStart(indices);
    }

    public long[] getTimesFromStart(int startIndex, int endIndex) {
        return getRoute().getTimesFromStart(startIndex, endIndex);
    }

    public long[] getTimesFromStart(int[] indices) {
        return getRoute().getTimesFromStart(indices);
    }

    public int[] getContainedPositions(BoundingBox boundingBox) {
        return getRoute().getContainedPositions(boundingBox);
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        return getRoute().getPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return getRoute().getInsignificantPositions(threshold);
    }

    public int getClosestPosition(double longitude, double latitude, double threshold) {
        return getRoute().getClosestPosition(longitude, latitude, threshold);
    }

    public int getClosestPosition(CompactCalendar time, long threshold) {
        return getRoute().getClosestPosition(time, threshold);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX, DATE_TIME_COLUMN_INDEX, DATE_COLUMN_INDEX, TIME_COLUMN_INDEX,
                    LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX, ELEVATION_COLUMN_INDEX, SPEED_COLUMN_INDEX -> true;
            default -> false;
        };
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        edit(rowIndex, new PositionColumnValues(columnIndex, aValue), true, true);
    }

    public void edit(int rowIndex, PositionColumnValues columnToValues, boolean fireEvent, boolean trackUndo) {
        // avoid exceptions due to parallel deletions
        if (rowIndex > getRowCount() - 1)
            return;

        if (columnToValues.getNextValues() != null) {
            for (int i = 0; i < columnToValues.getColumnIndices().size(); i++) {
                int columnIndex = columnToValues.getColumnIndices().get(i);
                positionsModelCallback.setValueAt(getPosition(rowIndex), columnIndex, columnToValues.getNextValues().get(i));
            }
        }

        if (fireEvent) {
            if (columnToValues.getColumnIndices().size() > 1)
                fireTableRowsUpdated(rowIndex, rowIndex);
            else
                fireTableRowsUpdated(rowIndex, rowIndex, columnToValues.getColumnIndices().get(0));
        }
    }

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, description);
        add(rowIndex, singletonList(position));
    }

    @SuppressWarnings("unchecked")
    public List<BaseNavigationPosition> createPositions(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        BaseNavigationFormat targetFormat = getRoute().getFormat();
        return convertPositions((List) route.getPositions(), targetFormat);
    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        List<BaseNavigationPosition> positions = createPositions(route);
        add(rowIndex, positions);
    }

    @SuppressWarnings({"unchecked"})
    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            BaseNavigationPosition position = positions.get(i);
            getRoute().add(rowIndex, position);
        }
        fireTableRowsInserted(rowIndex, rowIndex - 1 + positions.size());
    }

    public void remove(int firstIndex, int lastIndex) {
        int[] range = Range.asRange(firstIndex, lastIndex - 1);
        int[] rows = Range.revert(range);
        remove(rows);
    }

    public void remove(int[] rowIndices) {
        remove(rowIndices, true);
    }

    public void remove(int[] rows, final boolean fireEvent) {
        new ContinousRange(rows, new RangeOperation() {
            public void performOnIndex(int index) {
                getRoute().remove(index);
            }

            public void performOnRange(int firstIndex, int lastIndex) {
                if (fireEvent)
                    fireTableRowsDeleted(firstIndex, lastIndex);
            }

            public boolean isInterrupted() {
                return false;
            }
        }).performMonotonicallyDecreasing();
    }

    @SuppressWarnings("unchecked")
    public void sort(Comparator<NavigationPosition> comparator) {
        getRoute().sort(comparator);
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableModified();
    }

    @SuppressWarnings("unchecked")
    public void order(List<NavigationPosition> positions) {
        getRoute().order(positions);
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableModified();
    }

    public void revert() {
        getRoute().revert();
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableModified();
    }

    public void revert(int[] rowIndices) {
        Arrays.sort(rowIndices);
        getRoute().revert(rowIndices);
        fireTableRowsUpdated(rowIndices[0], rowIndices[rowIndices.length - 1]);
    }

    public void top(int[] rowIndices) {
        Arrays.sort(rowIndices);
        for (int i = 0; i < rowIndices.length; i++) {
            getRoute().top(rowIndices[i], i);
        }
        fireTableRowsUpdated(0, rowIndices[rowIndices.length - 1]);
    }

    public void topDown(int[] rowIndices) {
        int[] reverted = Range.revert(rowIndices);
        for (int row = 0; row < reverted.length; row++) {
            // move largest index with largest distance to top first
            for (int i = reverted.length - row - 1; i < reverted[row]; i++) {
                getRoute().move(i, i + 1);
            }
        }
        fireTableRowsUpdated(0, rowIndices[rowIndices.length - 1]);
    }

    public void up(int[] rowIndices, int delta) {
        Arrays.sort(rowIndices);
        for (int row : rowIndices) {
            // protect against IndexArrayOutOfBoundsException
            if(row - delta < 0)
                continue;

            getRoute().move(row, row - delta);
            fireTableRowsUpdated(row - delta, row);
        }
    }

    public void down(int[] rowIndices, int delta) {
        int[] reverted = Range.revert(rowIndices);
        for (int row : reverted) {
            // protect against IndexArrayOutOfBoundsException
            if(row + delta >= getRowCount())
                continue;

            getRoute().move(row, row + delta);
            fireTableRowsUpdated(row, row + delta);
        }
    }

    public void bottom(int[] rowIndices) {
        int[] reverted = Range.revert(rowIndices);
        for (int i = 0; i < reverted.length; i++) {
            getRoute().bottom(reverted[i], i);
            fireTableRowsUpdated(reverted[i], getRowCount() - 1 - i);
        }
    }

    public void bottomUp(int[] rows) {
        Arrays.sort(rows);
        for (int row = 0; row < rows.length; row++) {
            // move smallest index with largest distance to bottom first
            for (int i = getRowCount() - rows.length + row; i > rows[row]; i--) {
                getRoute().move(i, i - 1);
            }
        }
        fireTableRowsUpdated(rows[0], getRowCount() - 1);
    }

    private TableModelEvent currentEvent;

    public void fireTableChanged(TableModelEvent e) {
        this.currentEvent = e;
        super.fireTableChanged(e);
        this.currentEvent = null;
    }

    public boolean isContinousRangeOperation() {
        return currentEvent instanceof ContinousRangeTableModelEvent;
    }

    public boolean isFullTableModification() {
        return currentEvent instanceof FullTableModicationTableModelEvent;
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        fireTableChanged(new TableModelEvent(this, firstIndex, lastIndex, columnIndex, UPDATE));
    }

    public void fireTableRowsUpdatedInContinousRange(int firstIndex, int lastIndex, int columnIndex) {
        fireTableChanged(new ContinousRangeTableModelEvent(this, firstIndex, lastIndex, columnIndex, UPDATE));
    }

    public void fireTableRowsDeletedInContinousRange(int firstRow, int lastRow) {
        fireTableChanged(new ContinousRangeTableModelEvent(this, firstRow, lastRow, ALL_COLUMNS, DELETE));
    }

    public void fireTableModified() {
        fireTableChanged(new FullTableModicationTableModelEvent(this, 0, Integer.MAX_VALUE, ALL_COLUMNS, UPDATE));
    }

    private static class ContinousRangeTableModelEvent extends TableModelEvent {
        ContinousRangeTableModelEvent(TableModel source, int firstRow, int lastRow, int column, int type) {
            super(source, firstRow, lastRow, column, type);
        }
    }

    private static class FullTableModicationTableModelEvent extends TableModelEvent {
        FullTableModicationTableModelEvent(TableModel source, int firstRow, int lastRow, int column, int type) {
            super(source, firstRow, lastRow, column, type);
        }
    }
}

