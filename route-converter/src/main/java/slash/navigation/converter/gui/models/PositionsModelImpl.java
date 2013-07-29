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

import slash.common.io.Transfer;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationPosition;
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.PositionHelper;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.Range;
import slash.navigation.gui.events.RangeOperation;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.NavigationFormats.asFormatForPositions;
import static slash.navigation.common.UnitConversion.ddmm2latitude;
import static slash.navigation.common.UnitConversion.ddmm2longitude;
import static slash.navigation.common.UnitConversion.ddmmss2latitude;
import static slash.navigation.common.UnitConversion.ddmmss2longitude;
import static slash.navigation.converter.gui.helper.PositionHelper.extractComment;
import static slash.navigation.converter.gui.helper.PositionHelper.extractElevation;
import static slash.navigation.converter.gui.helper.PositionHelper.extractSpeed;
import static slash.navigation.converter.gui.helper.PositionHelper.extractTime;
import static slash.navigation.converter.gui.helper.PositionHelper.formatLatitude;
import static slash.navigation.converter.gui.helper.PositionHelper.formatLongitude;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DISTANCE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.SPEED_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.TIME_COLUMN_INDEX;

/**
 * Implements the {@link PositionsModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsModelImpl extends AbstractTableModel implements PositionsModel {
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

    public String getStringAt(int rowIndex, int columnIndex) {
        NavigationPosition position = getPosition(rowIndex);
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX:
                return extractComment(position);
            case TIME_COLUMN_INDEX:
                return extractTime(position);
            case LONGITUDE_COLUMN_INDEX:
                return formatLongitude(position.getLongitude());
            case LATITUDE_COLUMN_INDEX:
                return formatLatitude(position.getLatitude());
            case ELEVATION_COLUMN_INDEX:
                return extractElevation(position);
            case SPEED_COLUMN_INDEX:
                return extractSpeed(position);
        }
        throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
    }

    private double[] distanceCache = null;

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case DISTANCE_COLUMN_INDEX:
                if (distanceCache == null)
                    distanceCache = getRoute().getDistancesFromStart(0, getRowCount() - 1);
                return distanceCache[rowIndex];
            case ELEVATION_ASCEND_COLUMN_INDEX:
                return getRoute().getElevationAscend(0, rowIndex);
            case ELEVATION_DESCEND_COLUMN_INDEX:
                return getRoute().getElevationDescend(0, rowIndex);
        }
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
        List<NavigationPosition> result = new ArrayList<NavigationPosition>(rowIndices.length);
        for (int rowIndex : rowIndices)
            result.add(getPosition(rowIndex));
        return result;
    }

    public List<NavigationPosition> getPositions(int firstIndex, int lastIndex) {
        List<NavigationPosition> result = new ArrayList<NavigationPosition>(lastIndex - firstIndex);
        for (int i = firstIndex; i < lastIndex; i++)
            result.add(getPosition(i));
        return result;
    }

    public int[] getContainedPositions(NavigationPosition northEastCorner, NavigationPosition southWestCorner) {
        return getRoute().getContainedPositions(northEastCorner, southWestCorner);
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

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX:
            case TIME_COLUMN_INDEX:
            case LONGITUDE_COLUMN_INDEX:
            case LATITUDE_COLUMN_INDEX:
            case ELEVATION_COLUMN_INDEX:
            case SPEED_COLUMN_INDEX:
                return true;
            default:
                return false;
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        edit(rowIndex, columnIndex, aValue, -1, null, true, true);
    }

    public void edit(int rowIndex, int firstColumnIndex, Object firstValue, int secondColumnIndex, Object secondValue, boolean fireEvent, boolean trackUndo) {
        if (rowIndex == getRowCount())
            return;

        editCell(rowIndex, firstColumnIndex, firstValue);
        if (secondColumnIndex != -1)
            editCell(rowIndex, secondColumnIndex, secondValue);

        if (fireEvent) {
            if (secondColumnIndex != -1)
                fireTableRowsUpdated(rowIndex, rowIndex);
            else
                fireTableRowsUpdated(rowIndex, rowIndex, firstColumnIndex);
        }
    }

    private void editCell(int rowIndex, int columnIndex, Object value) {
        NavigationPosition position = getPosition(rowIndex);
        String string = value != null ? trim(value.toString()) : null;
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX:
                position.setComment(string);
                break;
            case TIME_COLUMN_INDEX:
                position.setTime(parseTime(value, string));
                break;
            case LONGITUDE_COLUMN_INDEX:
                position.setLongitude(parseLongitude(value, string));
                break;
            case LATITUDE_COLUMN_INDEX:
                position.setLatitude(parseLatitude(value, string));
                break;
            case ELEVATION_COLUMN_INDEX:
                Double elevation = parseElevation(value, string);
                position.setElevation(elevation);
                break;
            case SPEED_COLUMN_INDEX:
                Double speed = parseSpeed(value, string);
                position.setSpeed(speed);
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
    }

    private Double parseLongitude(Object objectValue, String stringValue) {
        DegreeFormat degreeFormat = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        switch (degreeFormat) {
            case Degrees:
                return parseDouble(objectValue, stringValue, null);
            case Degrees_Minutes:
                return ddmm2longitude(stringValue);
            case Degrees_Minutes_Seconds:
                return ddmmss2longitude(stringValue);
            default:
                throw new IllegalArgumentException("Degree format " + degreeFormat + " does not exist");
        }
    }

    private Double parseLatitude(Object objectValue, String stringValue) {
        DegreeFormat degreeFormat = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        switch (degreeFormat) {
            case Degrees:
                return parseDouble(objectValue, stringValue, null);
            case Degrees_Minutes:
                return ddmm2latitude(stringValue);
            case Degrees_Minutes_Seconds:
                return ddmmss2latitude(stringValue);
            default:
                throw new IllegalArgumentException("Degree format " + degreeFormat + " does not exist");
        }
    }

    private Double parseDouble(Object objectValue, String stringValue, String replaceAll) {
        if (objectValue == null || objectValue instanceof Double) {
            return (Double) objectValue;
        } else {
            if (replaceAll != null && stringValue != null)
                stringValue = stringValue.replaceAll(replaceAll, "");
            return Transfer.parseDouble(stringValue);
        }
    }

    private Double parseElevation(Object objectValue, String stringValue) {
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        Double value = parseDouble(objectValue, stringValue, unitSystem.getElevationName());
        return unitSystem.valueToDefault(value);
    }

    private Double parseSpeed(Object objectValue, String stringValue) {
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        Double value = parseDouble(objectValue, stringValue, unitSystem.getSpeedName());
        return unitSystem.valueToDefault(value);
    }

    private CompactCalendar parseTime(Object objectValue, String stringValue) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                return PositionHelper.parseTime(stringValue);
            } catch (ParseException e) {
                // intentionally left empty
            }
        }
        return null;
    }

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, comment);
        add(rowIndex, asList(position));
    }

    @SuppressWarnings("unchecked")
    public List<BaseNavigationPosition> createPositions(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        BaseNavigationFormat targetFormat = getRoute().getFormat();
        return asFormatForPositions((List)route.getPositions(), targetFormat);
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
        sort(rowIndices);

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
        sort(rowIndices);

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
        sort(rows);

        for (int i = 0; i < rows.length; i++) {
            getRoute().up(getRowCount() - rows.length + i, rows[i]);
        }
        fireTableRowsUpdated(rows[0], getRowCount() - 1);
    }

    public void fireTableChanged(TableModelEvent e) {
        distanceCache = null;
        super.fireTableChanged(e);
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        fireTableChanged(new TableModelEvent(this, firstIndex, lastIndex, columnIndex, UPDATE));
    }
}