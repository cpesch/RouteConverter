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

import slash.navigation.converter.gui.models.PositionColumnValues;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Acts as a {@link UndoableEdit} for editing positions of a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

class EditPosition extends AbstractUndoableEdit {
    private final UndoPositionsModel positionsModel;
    private final int row;
    private final PositionColumnValues columnToValues;

    public EditPosition(UndoPositionsModel positionsModel, int row, PositionColumnValues columnToValues) {
        this.positionsModel = positionsModel;
        this.row = row;
        this.columnToValues = columnToValues;
    }

    public String getUndoPresentationName() {
        return "edit-position-undo";
    }

    public String getRedoPresentationName() {
        return "edit-position-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        positionsModel.edit(row, new PositionColumnValues(columnToValues.getColumnIndices(), columnToValues.getPreviousValues()), true, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        positionsModel.edit(row, new PositionColumnValues(columnToValues.getColumnIndices(), columnToValues.getNextValues()), true, false);
    }
}
