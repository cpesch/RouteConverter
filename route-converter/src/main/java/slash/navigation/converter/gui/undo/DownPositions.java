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

import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.events.Range;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Acts as a {@link UndoableEdit} for moving positions of a {@link PositionsModel} downwards.
 *
 * @author Christian Pesch
 */

class DownPositions extends AbstractUndoableEdit {
    private UndoPositionsModel positionsModel;
    private int[] rows;
    private int delta;

    public DownPositions(UndoPositionsModel positionsModel, int[] rows, int delta) {
        this.positionsModel = positionsModel;
        this.rows = rows;
        this.delta = delta;
    }

    public String getUndoPresentationName() {
        return "down-position-undo";
    }

    public String getRedoPresentationName() {
        return "down-position-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        positionsModel.up(Range.increment(rows, +delta), delta, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        positionsModel.down(rows, delta, false);
    }
}