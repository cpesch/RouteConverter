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

package slash.navigation.gui.undo;

import org.junit.Test;

import javax.swing.undo.AbstractUndoableEdit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for {@link UndoManager}.
 *
 * @author Christian Pesch
 */
public class UndoManagerTest {

    @Test
    public void freshManagerCanNeitherUndoNorRedo() {
        UndoManager sut = new UndoManager();

        assertFalse(sut.canUndo());
        assertFalse(sut.canRedo());
    }

    @Test
    public void addingAnEditEnablesUndo() {
        UndoManager sut = new UndoManager();

        sut.addEdit(new AbstractUndoableEdit());

        assertTrue(sut.canUndo());
        assertFalse(sut.canRedo());
    }

    @Test
    public void undoMovesTheEditOntoTheRedoStack() {
        UndoManager sut = new UndoManager();
        sut.addEdit(new AbstractUndoableEdit());

        sut.undo();

        assertFalse(sut.canUndo());
        assertTrue(sut.canRedo());

        sut.redo();

        assertTrue(sut.canUndo());
        assertFalse(sut.canRedo());
    }

    @Test
    public void discardAllEditsClearsBothStacks() {
        UndoManager sut = new UndoManager();
        sut.addEdit(new AbstractUndoableEdit());

        sut.discardAllEdits();

        assertFalse(sut.canUndo());
        assertFalse(sut.canRedo());
    }

    @Test
    public void everyMutatingOperationNotifiesChangeListeners() {
        UndoManager sut = new UndoManager();
        AtomicInteger changes = new AtomicInteger();
        sut.addChangeListener(e -> changes.incrementAndGet());

        sut.addEdit(new AbstractUndoableEdit());   // 1
        sut.undo();                                // 2
        sut.redo();                                // 3
        sut.discardAllEdits();                     // 4

        assertEquals(4, changes.get());
    }

    @Test
    public void presentationNamesAreNonNull() {
        UndoManager sut = new UndoManager();

        assertNotNull(sut.getUndoPresentationName());
        assertNotNull(sut.getRedoPresentationName());
    }
}
