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

import slash.common.io.*;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormats;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Implements the {@link PositionsModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsModelImpl extends AbstractTableModel implements PositionsModel {
    private static final DateFormat TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    static {
        TIME_FORMAT.setTimeZone(CompactCalendar.UTC);
    }
    private BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route;

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getRoute() {
        return route;
    }

    public void setRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        this.route = route;
        fireTableDataChanged();
    }

    public int getRowCount() {
        return getRoute() != null ? getRoute().getPositionCount() : 0;
    }

    public int getColumnCount() {
        throw new IllegalArgumentException("This is determined by the PositionsTableColumnModel");
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case PositionColumns.DISTANCE_COLUMN_INDEX:
                return getRoute().getDistance(0, rowIndex);
            case PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX:
                return getRoute().getElevationAscend(0, rowIndex);
            case PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX:
                return getRoute().getElevationDescend(0, rowIndex);
        }
        return getPosition(rowIndex);
    }

    public BaseNavigationPosition getPosition(int rowIndex) {
        return getRoute().getPosition(rowIndex);
    }

    public int getIndex(BaseNavigationPosition position) {
        return getRoute().getIndex(position);
    }

    public List<BaseNavigationPosition> getPositions(int[] rowIndices) {
        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>(rowIndices.length);
        for (int rowIndex : rowIndices)
            result.add(getPosition(rowIndex));
        return result;
    }

    public List<BaseNavigationPosition> getPositions(int firstIndex, int lastIndex) {
        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>(lastIndex - firstIndex);
        for (int i = firstIndex; i < lastIndex; i++)
            result.add(getPosition(i));
        return result;
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        return getRoute().getPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return getRoute().getInsignificantPositions(threshold);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case PositionColumns.DESCRIPTION_COLUMN_INDEX:
            case PositionColumns.TIME_COLUMN_INDEX:
            case PositionColumns.LONGITUDE_COLUMN_INDEX:
            case PositionColumns.LATITUDE_COLUMN_INDEX:
            case PositionColumns.ELEVATION_COLUMN_INDEX:
            case PositionColumns.SPEED_COLUMN_INDEX:
                return true;
            default:
                return false;
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        edit(aValue, rowIndex, columnIndex, true, true);
    }

    public void edit(Object aValue, int rowIndex, int columnIndex, boolean fireEvent, boolean trackUndo) {
        if (rowIndex == getRowCount())
            return;

        BaseNavigationPosition position = getPosition(rowIndex);
        String string = aValue != null ? Transfer.trim(aValue.toString()) : null;
        switch (columnIndex) {
            case PositionColumns.DESCRIPTION_COLUMN_INDEX:
                position.setComment(string);
                break;
            case PositionColumns.TIME_COLUMN_INDEX:
                position.setTime(parseDate(aValue, string));
                break;
            case PositionColumns.LONGITUDE_COLUMN_INDEX:
                Double longitude = parseDouble(aValue, string, null);
                if (longitude != null)
                    position.setLongitude(longitude);
                break;
            case PositionColumns.LATITUDE_COLUMN_INDEX:
                Double latitude = parseDouble(aValue, string, null);
                if (latitude != null)
                    position.setLatitude(latitude);
                break;
            case PositionColumns.ELEVATION_COLUMN_INDEX:
                Double elevation = parseDouble(aValue, string, "m");
                if (elevation != null)
                    position.setElevation(elevation);
                break;
            case PositionColumns.SPEED_COLUMN_INDEX:
                Double speed = parseDouble(aValue, string, "Km/h");
                if (speed != null)
                    position.setSpeed(speed);
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        if (fireEvent)
            fireTableRowsUpdated(rowIndex, rowIndex, columnIndex);
    }

    private CompactCalendar parseDate(Object objectValue, String stringValue) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                Date date = TIME_FORMAT.parse(stringValue);
                Calendar calendar = Calendar.getInstance(CompactCalendar.UTC);
                calendar.setTime(date);
                return CompactCalendar.fromCalendar(calendar);
            }
            catch (ParseException e) {
                // intentionally left empty
            }
        }
        return null;
    }

    private Double parseDouble(Object objectValue, String stringValue, String replaceAll) {
        if (objectValue == null || objectValue instanceof Double) {
            return (Double)objectValue;
        } else {
            try {
                if(replaceAll != null && stringValue != null)
                    stringValue = stringValue.replaceAll(replaceAll, "");
                return Transfer.parseDouble(stringValue);
            }
            catch (NumberFormatException e) {
                // intentionally left empty
            }
        }
        return null;
    }

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, comment);
        add(rowIndex, Arrays.asList(position));
    }

    public List<BaseNavigationPosition> createPositions(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        BaseNavigationFormat targetFormat = getRoute().getFormat();
        return NavigationFormats.asFormat(route.getPositions(), targetFormat);
    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        List<BaseNavigationPosition> positions = createPositions(route);
        add(rowIndex, positions);
    }

    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            BaseNavigationPosition position = positions.get(i);
            getRoute().add(rowIndex, position);
        }
        fireTableRowsInserted(rowIndex, rowIndex - 1 + positions.size());
    }

    public int[] createRowIndices(int from, int to) {
        int[] rows = new int[to - from];
        int count = 0;
        for (int i = to - 1; i >= from; i--)
            rows[count++] = i;
        return rows;
    }

    public void remove(int firstIndex, int lastIndex) {
        remove(createRowIndices(firstIndex, lastIndex));
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

    public void revert() {
        getRoute().revert();
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableRowsUpdated(-1, -1);
    }

    public void top(int[] rowIndices) {
        Arrays.sort(rowIndices);

        for (int i = 0; i < rowIndices.length; i++) {
            getRoute().top(rowIndices[i], i);
        }
        fireTableRowsUpdated(0, rowIndices[rowIndices.length - 1]);
    }

    public void topDown(int[] rows) {
        int[] reverted = Range.revert(rows);

        for (int i = 0; i < reverted.length; i++) {
            getRoute().down(reverted.length - i - 1, reverted[i]);
        }
        fireTableRowsUpdated(0, reverted[0]);
    }

    public void up(int[] rowIndices, int delta) {
        Arrays.sort(rowIndices);

        for (int row : rowIndices) {
            getRoute().up(row, row - delta);
            fireTableRowsUpdated(row - delta, row);
        }
    }

    public void down(int[] rowIndices, int delta) {
        int[] reverted = Range.revert(rowIndices);

        for (int row : reverted) {
            getRoute().down(row, row + delta);
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

        for (int i = 0; i < rows.length; i++) {
            getRoute().up(getRowCount() - rows.length + i, rows[i]);
        }
        fireTableRowsUpdated(rows[0], getRowCount() - 1);
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        fireTableChanged(new TableModelEvent(this, firstIndex, lastIndex, columnIndex, TableModelEvent.UPDATE));
    }
}