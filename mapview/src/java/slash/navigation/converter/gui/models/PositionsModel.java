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

import slash.navigation.*;
import slash.navigation.util.*;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;

/**
 * Acts as a {@link TableModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class PositionsModel extends AbstractTableModel {
    private static final DateFormat TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
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
        BaseNavigationPosition position = getPosition(rowIndex);
        switch (columnIndex) {
            case PositionColumns.DESCRIPTION_COLUMN_INDEX:
                return position.getComment();
            case PositionColumns.TIME_COLUMN_INDEX:
                CompactCalendar time = position.getTime();
                return time != null ? TIME_FORMAT.format(time.getTime()) : "";
            case PositionColumns.LONGITUDE_COLUMN_INDEX:
                return formatLongitudeOrLatitude(position.getLongitude());
            case PositionColumns.LATITUDE_COLUMN_INDEX:
                return formatLongitudeOrLatitude(position.getLatitude());
            case PositionColumns.ELEVATION_COLUMN_INDEX:
                return formatElevation(position.getElevation());
            case PositionColumns.SPEED_COLUMN_INDEX:
                return formatSpeed(position.getSpeed());
            case PositionColumns.DISTANCE_COLUMN_INDEX:
                return formatDistance(getRoute().getDistance(0, rowIndex));
            case PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX:
                return formatElevation(getRoute().getElevationAscend(0, rowIndex));
            case PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX:
                return formatElevation(getRoute().getElevationDescend(0, rowIndex));
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
    }

    public BaseNavigationPosition getPredecessor(BaseNavigationPosition position) {
        return getRoute().getPredecessor(position);
    }

    public BaseNavigationPosition getPosition(int rowIndex) {
        return getRoute().getPosition(rowIndex);
    }

    public int getIndex(BaseNavigationPosition position) {
        return getRoute().getIndex(position);
    }

    public BaseNavigationPosition getSuccessor(BaseNavigationPosition position) {
        return getRoute().getSuccessor(position);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        BaseNavigationPosition position = getPosition(rowIndex);
        String value = Transfer.trim(aValue.toString());
        switch(columnIndex) {
            case PositionColumns.DESCRIPTION_COLUMN_INDEX:
                position.setComment(value);
                break;
            case PositionColumns.TIME_COLUMN_INDEX:
                try {
                    Date date = TIME_FORMAT.parse(value);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    position.setTime(CompactCalendar.fromCalendar(calendar));
                }
                catch(ParseException e) {
                    // intentionally left empty
                }
                break;
            case PositionColumns.LONGITUDE_COLUMN_INDEX:
                try {
                    position.setLongitude(Transfer.parseDouble(value));
                }
                catch(NumberFormatException e) {
                    // intentionally left empty
                }
                break;
            case PositionColumns.LATITUDE_COLUMN_INDEX:
                try {
                    position.setLatitude(Transfer.parseDouble(value));
                } catch (NumberFormatException e) {
                    // intentionally left empty
                }
                break;
            case PositionColumns.ELEVATION_COLUMN_INDEX:
                try {
                    if (value != null)
                        value = value.replaceAll("m", "");
                    position.setElevation(Transfer.parseDouble(value));
                } catch (NumberFormatException e) {
                    // intentionally left empty
                }
                break;

            // only computed values
            case PositionColumns.DISTANCE_COLUMN_INDEX:
            case PositionColumns.ELEVATION_ASCEND_COLUMN_INDEX:
            case PositionColumns.ELEVATION_DESCEND_COLUMN_INDEX:
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
    }


    private String formatElevation(Double elevation) {
        return elevation != null ? Math.round(elevation) + " m" : "";
    }

    private String formatSpeed(Double speed) {
        if (speed == null || speed == 0.0)
            return "";
        String speedStr;
        if (Math.abs(speed) < 10.0)
            speedStr = Double.toString(Transfer.roundFraction(speed, 1));
        else
            speedStr = Long.toString(Math.round(speed));
        return speedStr + " Km/h";
    }

    private String formatLongitudeOrLatitude(Double longitudeOrLatitude) {
        if (longitudeOrLatitude == null)
            return "";
        String result = Double.toString(longitudeOrLatitude) + " ";
        if (Math.abs(longitudeOrLatitude) < 10.0)
            result = " " + result;
        if (Math.abs(longitudeOrLatitude) < 100.0)
            result = " " + result;
        if (result.length() > 12)
            result = result.substring(0, 12 - 1);
        return result;
    }

    private String formatDistance(double distance) {
        double rounded = Transfer.roundFraction(distance / 1000.0, 1);
        return rounded > 0.0 ? rounded + " Km" : "";
    }


    public void top(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            getRoute().top(rows[i], i);
            fireTableRowsUpdated(i, rows[i]);
        }
    }

    public void up(int[] rows) {
        Arrays.sort(rows);

        for (int row : rows) {
            getRoute().up(row);
            fireTableRowsUpdated(row - 1, row);
        }
    }

    public void add(int row, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, comment);
        add(row, position);
    }

    public void add(int row, BaseNavigationPosition position) {
        getRoute().add(row, position);
        fireTableRowsInserted(row, row);
    }

    public void add(int row, List<BaseNavigationPosition> positions) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            BaseNavigationPosition position = positions.get(i);
            getRoute().add(row, position);
        }
        fireTableRowsInserted(row, row - 1 + positions.size());
    }

    public void add(int row, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        BaseNavigationFormat targetFormat = getRoute().getFormat();
        List<BaseNavigationPosition> positions = new ArrayList<BaseNavigationPosition>();
        for (BaseNavigationPosition sourcePosition : route.getPositions()) {
            BaseNavigationPosition targetPosition = NavigationFormats.asFormat(sourcePosition, targetFormat);
            positions.add(targetPosition);
        }
        add(row, positions);
    }

    public List<BaseNavigationPosition> remove(int from, int to) {
        List<BaseNavigationPosition> removed = new ArrayList<BaseNavigationPosition>();
        for (int i = to; i > from; i--)
            removed.add(0, getRoute().remove(i));
        fireTableRowsDeleted(from, to);
        return removed;
    }

    public void remove(int[] rows) {
        new ContinousRange(rows, new ContinousRange.RangeOperation() {
            public void performOnIndex(int index) {
                getRoute().remove(index);
            }
            public void performOnRange(int firstIndex, int lastIndex) {
                fireTableRowsDeleted(firstIndex, lastIndex);
            }
        }).performMonotonicallyDecreasing();
    }

    public int[] getDuplicatesWithinDistance(double distance) {
        return getRoute().getDuplicatesWithinDistance(distance);
    }

    public int[] getPositionsThatRemainingHaveDistance(double distance) {
        return getRoute().getPositionsThatRemainingHaveDistance(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return getRoute().getInsignificantPositions(threshold);
    }

    public void revert() {
        getRoute().revert();
        fireTableDataChanged();
    }

    public void down(int[] rows) {
        int[] reverted = Range.revert(rows);

        for (int row : reverted) {
            getRoute().down(row);
            fireTableRowsUpdated(row, row + 1);
        }
    }

    public void bottom(int[] rows) {
        int[] reverted = Range.revert(rows);

        for (int i = 0; i < reverted.length; i++) {
            getRoute().bottom(reverted[i], i);
            fireTableRowsUpdated(reverted[i], getRowCount() - 1 - i);
        }
    }
}
