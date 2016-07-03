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
import java.util.ArrayList;
import java.util.List;

/**
 * Acts as a {@link UndoableEdit} for removing positions from {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

class RemovePositions extends AbstractUndoableEdit {
    private UndoPositionsModel positionsModel;
    private List<Integer> rowList = new ArrayList<>();
    private List<List<NavigationPosition>> positionsList = new ArrayList<>();

    public RemovePositions(UndoPositionsModel positionsModel) {
        this.positionsModel = positionsModel;
    }

    public void add(int row, List<NavigationPosition> positions) {
        rowList.add(0, row);
        positionsList.add(0, positions);
    }

    public String getUndoPresentationName() {
        return "remove-position-undo";
    }

    public String getRedoPresentationName() {
        return "remove-position-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        for (int i = 0; i < rowList.size(); i++) {
            int row = rowList.get(i);
            List<NavigationPosition> positions = positionsList.get(i);
            positionsModel.add(row, positions, true, false);
        }
    }

    public void redo() throws CannotRedoException {
        super.redo();
        for (int i = rowList.size() - 1; i >= 0; i--) {
            int row = rowList.get(i);
            List<NavigationPosition> positions = positionsList.get(i);
            positionsModel.remove(row, row + positions.size(), true, false);
        }
    }
}