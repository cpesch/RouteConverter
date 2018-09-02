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
import slash.navigation.common.DegreeFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.PositionHelper;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.Range;
import slash.navigation.gui.events.RangeOperation;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.DELETE;
import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.NavigationFormatConverter.convertPositions;
import static slash.navigation.common.UnitConversion.ddmm2latitude;
import static slash.navigation.common.UnitConversion.ddmm2longitude;
import static slash.navigation.common.UnitConversion.ddmmss2latitude;
import static slash.navigation.common.UnitConversion.ddmmss2longitude;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractDate;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractDateTime;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractElevation;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractPattern;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractSpeed;
import static slash.navigation.converter.gui.helpers.PositionHelper.extractTime;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatLatitude;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatLongitude;
import static slash.navigation.converter.gui.helpers.PositionHelper.getDateFormat;
import static slash.navigation.converter.gui.helpers.PositionHelper.getDateTimeFormat;
import static slash.navigation.converter.gui.helpers.PositionHelper.getTimeFormat;
import static slash.navigation.converter.gui.models.PositionColumns.DATE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DATE_TIME_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
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
                return position.getDescription();
            case DATE_TIME_COLUMN_INDEX:
                return extractDateTime(position);
            case DATE_COLUMN_INDEX:
                return extractDate(position);
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
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX:
            case DATE_TIME_COLUMN_INDEX:
            case DATE_COLUMN_INDEX:
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
        edit(rowIndex, new PositionColumnValues(columnIndex, aValue), true, true);
    }

    public void edit(int rowIndex, PositionColumnValues columnToValues, boolean fireEvent, boolean trackUndo) {
        if (rowIndex == getRowCount())
            return;

        if (columnToValues.getNextValues() != null) {
            for (int i = 0; i < columnToValues.getColumnIndices().size(); i++) {
                int columnIndex = columnToValues.getColumnIndices().get(i);
                editCell(rowIndex, columnIndex, columnToValues.getNextValues().get(i));
            }
        }

        if (fireEvent) {
            if (columnToValues.getColumnIndices().size() > 1)
                fireTableRowsUpdated(rowIndex, rowIndex);
            else
                fireTableRowsUpdated(rowIndex, rowIndex, columnToValues.getColumnIndices().get(0));
        }
    }

    private void editCell(int rowIndex, int columnIndex, Object value) {
        NavigationPosition position = getPosition(rowIndex);
        String string = value != null ? trim(value.toString()) : null;
        switch (columnIndex) {
            case DESCRIPTION_COLUMN_INDEX:
                position.setDescription(string);
                break;
            case DATE_TIME_COLUMN_INDEX:
                position.setTime(parseDateTime(value, string));
                break;
            case DATE_COLUMN_INDEX:
                position.setTime(parseDate(value, string, position.getTime()));
                break;
            case TIME_COLUMN_INDEX:
                position.setTime(parseTime(value, string, position.getTime()));
                break;
            case LONGITUDE_COLUMN_INDEX:
                position.setLongitude(parseLongitude(value, string));
                break;
            case LATITUDE_COLUMN_INDEX:
                position.setLatitude(parseLatitude(value, string));
                break;
            case ELEVATION_COLUMN_INDEX:
                position.setElevation(parseElevation(value, string));
                break;
            case SPEED_COLUMN_INDEX:
                position.setSpeed(parseSpeed(value, string));
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
    }

    private Double parseLongitude(Object objectValue, String stringValue) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;

        DegreeFormat degreeFormat = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        switch (degreeFormat) {
            case Degrees:
                return parseDouble(stringValue);
            case Degrees_Minutes:
                return ddmm2longitude(stringValue);
            case Degrees_Minutes_Seconds:
                return ddmmss2longitude(stringValue);
            default:
                throw new IllegalArgumentException("Degree format " + degreeFormat + " does not exist");
        }
    }

    private Double parseLatitude(Object objectValue, String stringValue) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;

        DegreeFormat degreeFormat = RouteConverter.getInstance().getUnitSystemModel().getDegreeFormat();
        switch (degreeFormat) {
            case Degrees:
                return parseDouble(stringValue);
            case Degrees_Minutes:
                return ddmm2latitude(stringValue);
            case Degrees_Minutes_Seconds:
                return ddmmss2latitude(stringValue);
            default:
                throw new IllegalArgumentException("Degree format " + degreeFormat + " does not exist");
        }
    }

    private Double parseDegrees(Object objectValue, String stringValue, String replaceAll) {
        if (objectValue == null || objectValue instanceof Double)
            return (Double) objectValue;
        if (replaceAll != null && stringValue != null)
            stringValue = stringValue.replaceAll(replaceAll, "");
        return parseDouble(stringValue);
    }

    private Double parseElevation(Object objectValue, String stringValue) {
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        Double value = parseDegrees(objectValue, stringValue, unitSystem.getElevationName());
        return unitSystem.valueToDefault(value);
    }

    private Double parseSpeed(Object objectValue, String stringValue) {
        UnitSystem unitSystem = RouteConverter.getInstance().getUnitSystemModel().getUnitSystem();
        Double value = parseDegrees(objectValue, stringValue, unitSystem.getSpeedName());
        return unitSystem.distanceToDefault(value);
    }

    private void handleDateTimeParseException(String stringValue, String messageBundleKey, DateFormat format) {
        showMessageDialog(RouteConverter.getInstance().getFrame(),
                MessageFormat.format(RouteConverter.getBundle().getString(messageBundleKey),
                        stringValue, extractPattern(format)),
                RouteConverter.getTitle(), ERROR_MESSAGE);
    }

    private CompactCalendar parseDateTime(Object objectValue, String stringValue) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                return PositionHelper.parseDateTime(stringValue);
            } catch (ParseException e) {
                handleDateTimeParseException(stringValue, "date-time-format-error", getDateTimeFormat());
            }
        }
        return null;
    }

    private CompactCalendar parseDate(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                CompactCalendar result = PositionHelper.parseDate(stringValue);
                if(positionTime != null) {
                    Calendar calendar = positionTime.getCalendar();
                    calendar.set(DAY_OF_MONTH, result.getCalendar().get(DAY_OF_MONTH));
                    calendar.set(MONTH, result.getCalendar().get(MONTH));
                    calendar.set(YEAR, result.getCalendar().get(YEAR));
                    result = fromCalendar(calendar);
                }
                return result;
            } catch (ParseException e) {
                handleDateTimeParseException(stringValue, "date-format-error", getDateFormat());
            }
        }
        return null;
    }

    private CompactCalendar parseTime(Object objectValue, String stringValue, CompactCalendar positionTime) {
        if (objectValue == null || objectValue instanceof CompactCalendar) {
            return (CompactCalendar) objectValue;
        } else if (stringValue != null) {
            try {
                CompactCalendar result = PositionHelper.parseTime(stringValue);
                if (positionTime != null) {
                    Calendar calendar = positionTime.getCalendar();
                    calendar.set(HOUR_OF_DAY, result.getCalendar().get(HOUR_OF_DAY));
                    calendar.set(MINUTE, result.getCalendar().get(MINUTE));
                    calendar.set(SECOND, result.getCalendar().get(SECOND));
                    result = fromCalendar(calendar);
                }
                return result;
            } catch (ParseException e) {
                handleDateTimeParseException(stringValue, "time-format-error", getTimeFormat());

            }
        }
        return null;
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

    @SuppressWarnings("unchecked")
    public void sort(Comparator<NavigationPosition> comparator) {
        getRoute().sort(comparator);
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableRowsUpdated(0, MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    public void order(List<NavigationPosition> positions) {
        getRoute().order(positions);
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableRowsUpdated(0, MAX_VALUE);
    }

    public void revert() {
        getRoute().revert();
        // since fireTableDataChanged(); is ignored in FormatAndRoutesModel#setModified(true) logic
        fireTableRowsUpdated(0, MAX_VALUE);
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
            getRoute().move(reverted.length - i - 1, reverted[i]);
        }
        fireTableRowsUpdated(0, reverted[0]);
    }

    public void up(int[] rowIndices, int delta) {
        Arrays.sort(rowIndices);

        for (int row : rowIndices) {
            getRoute().move(row, row - delta);
            fireTableRowsUpdated(row - delta, row);
        }
    }

    public void down(int[] rowIndices, int delta) {
        int[] reverted = Range.revert(rowIndices);

        for (int row : reverted) {
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

        for (int i = 0; i < rows.length; i++) {
            getRoute().move(getRowCount() - rows.length + i, rows[i]);
        }
        fireTableRowsUpdated(rows[0], getRowCount() - 1);
    }

    private TableModelEvent currentEvent;

    public void fireTableChanged(TableModelEvent e) {
        this.currentEvent = e;
        super.fireTableChanged(e);
        this.currentEvent = null;
    }

    public boolean isContinousRange() {
        return currentEvent instanceof ContinousRangeTableModelEvent;
    }

    public void fireTableRowsDeletedInContinousRange(int firstRow, int lastRow) {
        fireTableChanged(new ContinousRangeTableModelEvent(this, firstRow, lastRow, ALL_COLUMNS, DELETE));
    }

    private static class ContinousRangeTableModelEvent extends TableModelEvent {
        @SuppressWarnings("MagicConstant")
        ContinousRangeTableModelEvent(TableModel source, int firstRow, int lastRow, int column, int type) {
            super(source, firstRow, lastRow, column, type);
        }
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        fireTableChanged(new TableModelEvent(this, firstIndex, lastIndex, columnIndex, UPDATE));
    }
}
