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
        edit(aValue, rowIndex, columnIndex, true, true);
    }

    public void edit(Object aValue, int rowIndex, int columnIndex, boolean fireEvent, boolean trackUndo) {
        if (rowIndex == getRowCount())
            return;

        Object previousValue = trackUndo ? getValueAt(rowIndex, columnIndex) : null;
        delegate.edit(aValue, rowIndex, columnIndex, fireEvent, trackUndo);
        if (trackUndo)
            undoManager.addEdit(new EditPosition(this, rowIndex, columnIndex, previousValue, aValue));
    }

    public void addTableModelListener(TableModelListener l) {
        delegate.addTableModelListener(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        delegate.removeTableModelListener(l);
    }

    public void fireTableRowsUpdated(int firstIndex, int lastIndex, int columnIndex) {
        delegate.fireTableRowsUpdated(firstIndex, lastIndex, columnIndex);
    }

    public int getNearestPositionsToCoordinates( double longitude, double latitude) {
        return delegate.getNearestPositionsToCoordinates(longitude, latitude);
    }
    public int getNearestPositionsToCoordinatesWithinDistance( double longitude, double latitude, double distance) {
        return delegate.getNearestPositionsToCoordinatesWithinDistance(longitude, latitude, distance);
    }

    public int[] getPositionsWithinRectangle(double longitudeNE, double latitudeNE, double longitudeSW, double latitudeSW)
    {
        return getRoute().getPositionsWithinRectangle( longitudeNE, latitudeNE, longitudeSW, latitudeSW);
    }

    // PositionsModel

    public BaseRoute<BaseNavigationPosition, BaseNavigationFormat> getRoute() {
        return delegate.getRoute();
    }

    public void setRoute(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        delegate.setRoute(route);
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

    public List<BaseNavigationPosition> getPositions(int firstIndex, int lastIndex) {
        return delegate.getPositions(firstIndex, lastIndex);
    }

    public int[] getPositionsWithinDistanceToPredecessor(double distance) {
        return delegate.getPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] getInsignificantPositions(double threshold) {
        return delegate.getInsignificantPositions(threshold);
    }

    // Undoable operations

    public void add(int rowIndex, Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        BaseNavigationPosition position = getRoute().createPosition(longitude, latitude, elevation, speed, time, comment);
        add(rowIndex, Arrays.asList(position));

    }

    public void add(int rowIndex, BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) throws IOException {
        List<BaseNavigationPosition> positions = delegate.createPositions(route);
        add(rowIndex, positions);
    }

    public void add(int rowIndex, List<BaseNavigationPosition> positions) {
        add(rowIndex, positions, true, true);
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
            public boolean isInterrupted() {
                return false;
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

    public void top(int[] rowIndices) {
        top(rowIndices, true);
    }

    void top(int[] rows, boolean trackUndo) {
        delegate.top(rows);
        if(trackUndo)
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
        if(trackUndo)
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
