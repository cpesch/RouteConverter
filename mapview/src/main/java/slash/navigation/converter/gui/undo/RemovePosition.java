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

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.undo.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.List;

/**
 * Acts as a {@link UndoableEdit} for adding positions to {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

public class RemovePosition extends AbstractUndoableEdit {
    private PositionsModel positionsModel;
    private int row;
    private List<BaseNavigationPosition> positions;

    public RemovePosition(PositionsModel positionsModel, int row, List<BaseNavigationPosition> positions) {
        this.positionsModel = positionsModel;
        this.row = row;
        this.positions = positions;
    }

    public String getUndoPresentationName() {
        return "Add removed positions";        // TODO localize
    }

    public String getRedoPresentationName() {
        return "Removed added positions";       // TODO localize
    }

    public void undo() throws CannotUndoException {
        positionsModel.remove(row, row + positions.size());
    }

    public void redo() throws CannotRedoException {
        positionsModel.add(row, positions);
    }
}