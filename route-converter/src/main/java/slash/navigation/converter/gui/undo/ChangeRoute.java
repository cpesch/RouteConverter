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
 * Acts as a {@link UndoableEdit} for changing the route of a {@link UndoFormatAndRoutesModel}.
 *
 * @author Christian Pesch
 */

class ChangeRoute extends AbstractUndoableEdit {
    private final UndoFormatAndRoutesModel formatAndRoutesModel;
    private final BaseRoute previousRoute;
    private final BaseRoute nextRoute;

    public ChangeRoute(UndoFormatAndRoutesModel formatAndRoutesModel, BaseRoute previousRoute, BaseRoute nextRoute) {
        this.formatAndRoutesModel = formatAndRoutesModel;
        this.previousRoute = previousRoute;
        this.nextRoute = nextRoute;
    }

    public boolean isSignificant() {
        return false;
    }

    public void undo() throws CannotUndoException {
        super.undo();
        formatAndRoutesModel.setSelectedRoute(previousRoute, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        formatAndRoutesModel.setSelectedRoute(nextRoute, false);
    }
}
