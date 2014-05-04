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
import java.util.Comparator;
import java.util.List;

/**
 * Acts as a {@link UndoableEdit} for sorting the positions of a {@link PositionsModel}.
 *
 * @author Christian Pesch
 */

class SortPositions extends AbstractUndoableEdit {
    private UndoPositionsModel positionsModel;
    private Comparator<NavigationPosition> comparator;
    private List<NavigationPosition> positions;

    public SortPositions(UndoPositionsModel positionsModel, Comparator<NavigationPosition> comparator, List<NavigationPosition> positions) {
        this.positionsModel = positionsModel;
        this.comparator = comparator;
        this.positions = positions;
    }

    public String getUndoPresentationName() {
        return "sort-position-undo";
    }

    public String getRedoPresentationName() {
        return "sort-position-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        positionsModel.order(positions);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        positionsModel.sort(comparator, false);
    }
}