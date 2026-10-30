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

import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModelCallback;
import slash.navigation.converter.gui.panels.PositionsModelCallbackImpl;
import slash.navigation.gui.undo.UndoManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;

public class UndoPositionsModelTest {
    UndoManager undoManager = new UndoManager();
    UndoPositionsModel model = new UndoPositionsModel(undoManager, new PositionsModelCallbackImpl(null));
    BaseRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
    BcrPosition a = new BcrPosition(1, 1, 0, "a");
    BcrPosition b = new BcrPosition(3, 3, 0, "b");
    BcrPosition c = new BcrPosition(5, 5, 0, "c");
    BcrPosition d = new BcrPosition(7, 7, 0, "d");
    BcrPosition e = new BcrPosition(9, 9, 0, "e");

    @SuppressWarnings("unchecked")
    private void initialize() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        model.setRoute(route);
    }

    private String descriptions() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < model.getRowCount(); i++)
            result.append(model.getPosition(i).getDescription());
        return result.toString();
    }

    @Test
    public void testRemoveWithScatteredIndicesAndUndo() {
        initialize();
        // two separate single-element ranges, not one contiguous block
        model.remove(new int[]{1, 3});
        assertEquals(3, model.getRowCount());
        assertEquals("ace", descriptions());

        undoManager.undo();
        assertEquals(5, model.getRowCount());
        assertEquals("abcde", descriptions());

        undoManager.redo();
        assertEquals(3, model.getRowCount());
        assertEquals("ace", descriptions());
    }

    @Test
    public void testRemoveWithContiguousRangeAndUndo() {
        initialize();
        model.remove(new int[]{1, 2, 3});
        assertEquals(2, model.getRowCount());
        assertEquals("ae", descriptions());

        undoManager.undo();
        assertEquals(5, model.getRowCount());
        assertEquals("abcde", descriptions());

        undoManager.redo();
        assertEquals(2, model.getRowCount());
        assertEquals("ae", descriptions());
    }

    // a value-rejecting PositionsModelCallback, to exercise #309 without depending on
    // PositionsModelCallbackImpl's own format parsing (degree/unit formats, time zone)
    private static final String INVALID = "invalid";

    private static PositionsModelCallback rejectingCallback() {
        return new PositionsModelCallback() {
            public String getStringAt(NavigationPosition position, int columnIndex) {
                return position.getDescription();
            }

            public boolean setValueAt(NavigationPosition position, int columnIndex, Object value) {
                if (INVALID.equals(value))
                    return false;
                position.setDescription((String) value);
                return true;
            }
        };
    }

    @Test
    public void testInvalidEditAddsNoUndoStepAndFiresNoEvent() {
        initialize();
        UndoManager localUndoManager = new UndoManager();
        UndoPositionsModel rejectingModel = new UndoPositionsModel(localUndoManager, rejectingCallback());
        rejectingModel.setRoute(route);

        final int[] eventCount = {0};
        rejectingModel.addTableModelListener(e -> eventCount[0]++);

        rejectingModel.setValueAt(INVALID, 0, DESCRIPTION_COLUMN_INDEX);

        assertFalse(localUndoManager.canUndo());
        assertEquals(0, eventCount[0]);
        assertEquals("a", rejectingModel.getPosition(0).getDescription());
    }

    @Test
    public void testValidEditAddsOneUndoStepAndFiresOneEvent() {
        initialize();
        UndoManager localUndoManager = new UndoManager();
        UndoPositionsModel acceptingModel = new UndoPositionsModel(localUndoManager, rejectingCallback());
        acceptingModel.setRoute(route);

        final int[] eventCount = {0};
        acceptingModel.addTableModelListener(e -> eventCount[0]++);

        acceptingModel.setValueAt("z", 0, DESCRIPTION_COLUMN_INDEX);

        assertTrue(localUndoManager.canUndo());
        assertEquals(1, eventCount[0]);
        assertEquals("z", acceptingModel.getPosition(0).getDescription());
    }

    @Test
    public void testUnchangedDescriptionAddsNoUndoStepAndFiresNoEvent() {
        initialize();
        boolean canUndoBefore = undoManager.canUndo();

        final int[] eventCount = {0};
        model.addTableModelListener(e -> eventCount[0]++);

        model.setValueAt("a", 0, DESCRIPTION_COLUMN_INDEX);

        assertEquals(canUndoBefore, undoManager.canUndo());
        assertEquals(0, eventCount[0]);
        assertEquals("a", model.getPosition(0).getDescription());
    }

    @Test
    public void testChangedDescriptionAddsOneUndoStepAndFiresOneEvent() {
        initialize();

        final int[] eventCount = {0};
        model.addTableModelListener(e -> eventCount[0]++);

        model.setValueAt("z", 0, DESCRIPTION_COLUMN_INDEX);

        assertTrue(undoManager.canUndo());
        assertEquals(1, eventCount[0]);
        assertEquals("z", model.getPosition(0).getDescription());
    }
}
