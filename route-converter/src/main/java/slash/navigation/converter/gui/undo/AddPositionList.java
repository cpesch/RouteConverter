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

import slash.navigation.base.BaseRoute;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Acts as a {@link UndoableEdit} for adding the route of a {@link UndoFormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

class AddPositionList extends AbstractUndoableEdit {
    private final UndoFormatAndRoutesModel formatAndRoutesModel;
    private final int index;
    private final BaseRoute route;

    public AddPositionList(UndoFormatAndRoutesModel formatAndRoutesModel, int index, BaseRoute route) {
        this.formatAndRoutesModel = formatAndRoutesModel;
        this.index = index;
        this.route = route;
    }

    public String getUndoPresentationName() {
        return "add-position-list-undo";
    }

    public String getRedoPresentationName() {
        return "add-position-list-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        formatAndRoutesModel.removePositionList(route, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        formatAndRoutesModel.addPositionList(index, route, false);
    }
}
