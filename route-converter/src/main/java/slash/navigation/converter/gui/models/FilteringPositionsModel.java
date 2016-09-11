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
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.predicates.FilterPredicate;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.util.*;

import static slash.common.io.Transfer.toArray;

/**
 * Implements a {@link PositionsModel} that filters positions.
 *
 * @author Christian Pesch
 */

public class FilteringPositionsModel extends AbstractTableModel implements PositionsModel {
    private final PositionsModel delegate;
    private FilterPredicate predicate;
    private Map<Integer, Integer> mapping;

    public FilteringPositionsModel(PositionsModel delegate, FilterPredicate predicate) {
        this.delegate = delegate;
        this.predicate = predicate;
        initializeMapping();
        delegate.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                initializeMapping();
                fireTableDataChanged();
            }
        });
    }

    private void initializeMapping() {
        mapping = new HashMap<>();
        for (int i = 0, c = delegate.getRowCount(); i < c; i++) {
            NavigationPosition position = delegate.getPosition(i);
            if (predicate.shouldInclude(position)) {
                int mappedRow = mapping.size();
                mapping.put(mappedRow, i);
            }
        }
    }

    private int mapRow(int rowIndex) {
        Integer integer = mapping.get(rowIndex);
        return integer != null ? integer : -1;
    }

    public int[] mapRows(int[] rowIndices) {
        List<Integer> result = new ArrayList<>();
        for(int rowIndex : rowIndices) {
            int mappedRow = mapRow(rowIndex);
            if(mappedRow != -1)
                result.add(mappedRow);
        }
        return toArray(result);
    }

    public BaseRoute getRoute() {
        return delegate.getRoute();
    }

    public void setRoute(BaseRoute route) {
        delegate.setRoute(route);
    }

    public void setFilterPredicate(FilterPredicate predicate) {
        this.predicate = predicate;
        initializeMapping();
        fireTableDataChanged();
    }

    public int getRowCount() {
        return mapping.size();
    }

    public int getColumnCount() {
        throw new IllegalArgumentException("This is determined by the PositionsTableColumnModel");
    }

   public Object getValueAt(int rowIndex, int columnIndex) {
        return delegate.getValueAt(mapRow(rowIndex), columnIndex);
    }

    public NavigationPosition getPosition(int rowIndex) {
        return delegate.getPosition(mapRow(rowIndex));
    }

    public int getIndex(NavigationPosition position) {
        return mapRow(delegate.getIndex(position));
    }

    public List<NavigationPosition> getPositions(int[] rowIndices) {
        throw new UnsupportedOperationException();
    }

    public List<NavigationPosition> getPositions(int firstIndex, int lastIndex) {
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

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return delegate.isCellEditable(mapRow(rowIndex), columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        delegate.setValueAt(aValue, mapRow(rowIndex), columnIndex);
    }

    public void edit(int rowIndex, PositionColumnValues columnToValues, boolean fireEvent, boolean trackUndo) {
        delegate.edit(mapRow(rowIndex), columnToValues, fireEvent, trackUndo);
    }

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        throw new UnsupportedOperationException();
    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        throw new UnsupportedOperationException();
    }

    public void remove(int firstIndex, int lastIndex) {
        throw new UnsupportedOperationException();
    }

    public void remove(int[] rowIndices) {
        delegate.remove(mapRows(rowIndices));
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
        return delegate.isContinousRange();
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        delegate.fireTableRowsUpdated(firstIndex, lastIndex, columnIndex);
    }
}
