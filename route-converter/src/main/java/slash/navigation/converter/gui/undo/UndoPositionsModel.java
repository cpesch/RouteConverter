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

package slash.navigation.converter.gui.undo;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsModelImpl;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.Range;
import slash.navigation.gui.events.RangeOperation;
import slash.navigation.gui.undo.UndoManager;

import javax.swing.event.TableModelListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static slash.common.io.Transfer.trim;

/**
 * Implements a undo/redo-supporting {@link PositionsModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class UndoPositionsModel implements PositionsModel {
    private final PositionsModelImpl delegate = new PositionsModelImpl();
    private final UndoManager undoManager;

    public UndoPositionsModel(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    // TableModel

    public int getRowCount() {
        return delegate.getRowCount();
    }

    public int getColumnCount() {
        return delegate.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return delegate.getColumnName(columnIndex);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return delegate.getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return delegate.isCellEditable(rowIndex, columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return delegate.getValueAt(rowIndex, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        edit(rowIndex, columnIndex, aValue, -1, null, true, true);
    }

    public String getStringAt(int rowIndex, int columnIndex) {
        return delegate.getStringAt(rowIndex, columnIndex);
    }

    public void edit(int rowIndex, int firstColumnIndex, Object firstValue, int secondColumnIndex, Object secondValue, boolean fireEvent, boolean trackUndo) {
        if (rowIndex == getRowCount())
            return;

        Object previousFirstValue = trackUndo ? trim(getStringAt(rowIndex, firstColumnIndex)) : null;
        Object previousSecondValue = trackUndo && secondColumnIndex != -1 ? trim(getStringAt(rowIndex, secondColumnIndex)) : null;
        delegate.edit(rowIndex, firstColumnIndex, firstValue, secondColumnIndex, secondValue, fireEvent, trackUndo);
        if (trackUndo)
            undoManager.addEdit(new EditPosition(this, rowIndex, firstColumnIndex, previousFirstValue, firstValue, secondColumnIndex, previousSecondValue, secondValue));
    }

    public void addTableModelListener(TableModelListener l) {
        delegate.addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        delegate.removeTableModelListener(l);
    }

    private static final int CONTINOUS_RANGE_FINAL_EVENT = -2;

    public boolean isContinousRange() {
        return delegate.isContinousRange();
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        delegate.fireTableRowsUpdated(firstIndex, lastIndex, columnIndex);
    }

    // PositionsModel

    public BaseRoute getRoute() {
        return delegate.getRoute();
    }

    public void setRoute(BaseRoute route) {
        delegate.setRoute(route);
    }

    public NavigationPosition getPosition(int rowIndex) {
        return delegate.getPosition(rowIndex);
    }

    public int getIndex(NavigationPosition position) {
        return delegate.getIndex(position);
    }

    public List<NavigationPosition> getPositions(int[] rowIndices) {
        return delegate.getPositions(rowIndices);
    }

    public List<NavigationPosition> getPositions(int firstIndex, int lastIndex) {
        return delegate.getPositions(firstIndex, lastIndex);
    }

    public int[] getContainedPositions(BoundingBox boundingBox) {
        return delegate.getContainedPositions(boundingBox);
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        return delegate.getPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return delegate.getInsignificantPositions(threshold);
    }

    public int getClosestPosition(double longitude, double latitude, double threshold) {
        return delegate.getClosestPosition(longitude, latitude, threshold);
    }

    // Undoable operations

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, description);
        add(rowIndex, asList(position));

    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        List<BaseNavigationPosition> positions = delegate.createPositions(route);
        add(rowIndex, positions);
    }

    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        add(rowIndex, new ArrayList<NavigationPosition>(positions), true, true);
    }

    @SuppressWarnings("unchecked")
    void add(int row, List<NavigationPosition> positions, boolean fireEvent, boolean trackUndo) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            NavigationPosition position = positions.get(i);
            getRoute().add(row, (BaseNavigationPosition) position);
        }
        if (fireEvent)
            delegate.fireTableRowsInserted(row, row - 1 + positions.size());
        if (trackUndo)
            undoManager.addEdit(new AddPositions(this, row, positions));
    }

    public void remove(int firstIndex, int lastIndex) {
        remove(firstIndex, lastIndex, true, true);
    }

    public void remove(int[] rowIndices) {
        remove(rowIndices, true, true);
    }

    void remove(int from, int to, boolean fireEvent, boolean trackUndo) {
        int[] rows = delegate.createRowIndices(from, to);
        remove(rows, fireEvent, trackUndo);
    }

    void remove(int[] rows, final boolean fireEvent, final boolean trackUndo) {
        final RemovePositions edit = new RemovePositions(this);

        new ContinousRange(rows, new RangeOperation() {
            private List<NavigationPosition> removed = new ArrayList<NavigationPosition>();

            public void performOnIndex(int index) {
                removed.add(0, getRoute().remove(index));
            }

            public void performOnRange(int firstIndex, int lastIndex) {
                if (fireEvent)
                    delegate.fireTableRowsDeletedInContinousRange(firstIndex, lastIndex);
                if (trackUndo)
                    edit.add(firstIndex, removed);
                removed.clear();
            }

            public boolean isInterrupted() {
                return false;
            }
        }).performMonotonicallyDecreasing();

        if (fireEvent)
            fireTableRowsUpdated(0, MAX_VALUE, CONTINOUS_RANGE_FINAL_EVENT);
        if (trackUndo)
            undoManager.addEdit(edit);
    }

    public void sort(Comparator<NavigationPosition> comparator) {
        sort(comparator, true);
    }

    void sort(Comparator<NavigationPosition> comparator, boolean trackUndo) {
        @SuppressWarnings("unchecked")
        List<NavigationPosition> original = getRoute().getPositions();
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>(original);
        delegate.sort(comparator);
        if (trackUndo)
            undoManager.addEdit(new SortPositions(this, comparator, positions));
    }

    public void order(List<NavigationPosition> positions) {
        delegate.order(positions);
    }

    public void revert() {
        revert(true);
    }

    void revert(boolean trackUndo) {
        delegate.revert();
        if (trackUndo)
            undoManager.addEdit(new RevertPositions(this));
    }

    public void top(int[] rowIndices) {
        top(rowIndices, true);
    }

    void top(int[] rows, boolean trackUndo) {
        delegate.top(rows);
        if (trackUndo)
            undoManager.addEdit(new TopPositions(this, rows));
    }

    void topDown(int[] rows) {
        delegate.topDown(rows);
    }

    public void up(int[] rowIndices, int delta) {
        up(rowIndices, delta, true);
    }

    void up(int[] rows, int delta, boolean trackUndo) {
        delegate.up(rows, delta);
        if (trackUndo)
            undoManager.addEdit(new UpPositions(this, rows, delta));
    }

    public void down(int[] rowIndices, int delta) {
        down(rowIndices, delta, true);
    }

    void down(int[] rows, int delta, boolean trackUndo) {
        delegate.down(rows, delta);
        if (trackUndo)
            undoManager.addEdit(new DownPositions(this, Range.revert(rows), delta));
    }

    public void bottom(int[] rowIndices) {
        bottom(rowIndices, true);
    }

    void bottom(int[] rows, boolean trackUndo) {
        delegate.bottom(rows);
        if (trackUndo)
            undoManager.addEdit(new BottomPositions(this, Range.revert(rows)));
    }

    void bottomUp(int[] rows) {
        delegate.bottomUp(rows);
    }
}
