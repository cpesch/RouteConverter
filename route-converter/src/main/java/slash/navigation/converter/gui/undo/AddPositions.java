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

import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.util.List;

/**
 * Acts as a {@link UndoableEdit} for adding positions to {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

class AddPositions extends AbstractUndoableEdit {
    private UndoPositionsModel positionsModel;
    private int row;
    private List<NavigationPosition> positions;

    public AddPositions(UndoPositionsModel positionsModel, int row, List<NavigationPosition> positions) {
        this.positionsModel = positionsModel;
        this.row = row;
        this.positions = positions;
    }

    public String getUndoPresentationName() {
        return "add-position-undo";
    }

    public String getRedoPresentationName() {
        return "add-position-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        positionsModel.remove(row, row + positions.size(), true, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        positionsModel.add(row, positions, true, false);
    }
}
