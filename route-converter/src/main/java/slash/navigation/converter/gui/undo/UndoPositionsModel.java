package slash.navigation.converter.gui.undo;

import slash.common.io.CompactCalendar;
import slash.common.io.ContinousRange;
import slash.common.io.Range;
import slash.common.io.RangeOperation;
import slash.navigation.base.*;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsModelImpl;
import slash.navigation.gui.UndoManager;

import javax.swing.event.TableModelListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a undo/redo-supporting {@link PositionsModel} for the positions of a {@link BaseRoute}.
 *
 * @author Christian Pesch
 */

public class UndoPositionsModel implements PositionsModel {
    private PositionsModelImpl delegate = new PositionsModelImpl();
    private UndoManager undoManager;

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
        delegate.setValueAt(aValue, rowIndex, columnIndex);
    }

    public void addTableModelListener(TableModelListener l) {
        delegate.addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        delegate.removeTableModelListener(l);
    }

    public void fireTableRowsUpdated(int from, int to) {
        delegate.fireTableRowsUpdated(from, to);
    }

    public void fireTableDataChanged() {
        delegate.fireTableDataChanged();
    }

    // PositionsModel

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getRoute() {
        return delegate.getRoute();
    }

    public void setRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        delegate.setRoute(route);
    }

    public BaseNavigationPosition getPredecessor(BaseNavigationPosition position) {
        return delegate.getPredecessor(position);
    }

    public BaseNavigationPosition getPosition(int rowIndex) {
        return delegate.getPosition(rowIndex);
    }

    public int getIndex(BaseNavigationPosition position) {
        return delegate.getIndex(position);
    }

    public List<BaseNavigationPosition> getPositions(int[] rowIndices) {
        return delegate.getPositions(rowIndices);
    }

    public List<BaseNavigationPosition> getPositions(int from, int to) {
        return delegate.getPositions(from, to);
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        return delegate.getPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return delegate.getInsignificantPositions(threshold);
    }

    // Undoable operations

    public void add(int row, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, comment);
        add(row, Arrays.asList(position));

    }

    public void add(int row, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        List<BaseNavigationPosition> positions = delegate.createPositions(route);
        add(row, positions);
    }

    public void add(int row, List<BaseNavigationPosition> positions) {
        add(row, positions, true, true);
    }

    void add(int row, List<BaseNavigationPosition> positions, boolean fireEvent, boolean trackUndo) {
        for (int i = positions.size() - 1; i >= 0; i--) {
            BaseNavigationPosition position = positions.get(i);
            getRoute().add(row, position);
        }
        if (fireEvent)
            delegate.fireTableRowsInserted(row, row - 1 + positions.size());
        if (trackUndo)
            undoManager.addEdit(new AddPositions(this, row, positions));
    }

    public void remove(int from, int to) {
        remove(from, to, true, true);
    }

    public void remove(int[] rows) {
        remove(rows, true, true);
    }

    void remove(int from, int to, boolean fireEvent, boolean trackUndo) {
        int[] rows = delegate.createRowIndices(from, to);
        remove(rows, fireEvent, trackUndo);
    }

    void remove(int[] rows, final boolean fireEvent, final boolean trackUndo) {
        final RemovePositions edit = new RemovePositions(this);

        new ContinousRange(rows, new RangeOperation() {
            private List<BaseNavigationPosition> removed = new ArrayList<BaseNavigationPosition>();

            public void performOnIndex(int index) {
                removed.add(0, getRoute().remove(index));
            }

            public void performOnRange(int firstIndex, int lastIndex) {
                if (fireEvent)
                    delegate.fireTableRowsDeleted(firstIndex, lastIndex);
                if (trackUndo)
                    edit.add(firstIndex, new ArrayList<BaseNavigationPosition>(removed));
                removed.clear();
            }
        }).performMonotonicallyDecreasing();

        if (trackUndo)
            undoManager.addEdit(edit);
    }

    public void revert() {
        revert(true);
    }

    void revert(boolean trackUndo) {
        delegate.revert();
        if (trackUndo)
            undoManager.addEdit(new RevertPositions(this));
    }

    public void top(int[] rows) {
        top(rows, true);
    }

    void top(int[] rows, boolean trackUndo) {
        delegate.top(rows);
        if(trackUndo)
            undoManager.addEdit(new TopPositions(this, rows));
    }

    void topDown(int[] rows) {
        delegate.topDown(rows);
    }

    public void up(int[] rows) {
        up(rows, true);
    }

    void up(int[] rows, boolean trackUndo) {
        delegate.up(rows);
        if(trackUndo)
            undoManager.addEdit(new UpPositions(this, rows));
    }

    public void down(int[] rows) {
        down(rows, true);
    }

    void down(int[] rows, boolean trackUndo) {
        delegate.down(rows);
        if (trackUndo)
            undoManager.addEdit(new DownPositions(this, Range.revert(rows)));
    }

    public void bottom(int[] rows) {
        bottom(rows, true);
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
