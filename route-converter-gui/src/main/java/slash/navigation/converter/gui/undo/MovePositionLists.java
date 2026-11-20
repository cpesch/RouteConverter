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

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import static slash.navigation.converter.gui.helpers.RowMoves.inverse;

/**
 * Acts as a {@link UndoableEdit} for reordering the position lists of a {@link UndoFormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

class MovePositionLists extends AbstractUndoableEdit {
    private final UndoFormatAndRoutesModel formatAndRoutesModel;
    private final int[] order;

    public MovePositionLists(UndoFormatAndRoutesModel formatAndRoutesModel, int[] order) {
        this.formatAndRoutesModel = formatAndRoutesModel;
        this.order = order.clone();
    }

    public String getUndoPresentationName() {
        return "move-position-lists-undo";
    }

    public String getRedoPresentationName() {
        return "move-position-lists-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        formatAndRoutesModel.movePositionLists(inverse(order), false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        formatAndRoutesModel.movePositionLists(order, false);
    }
}
