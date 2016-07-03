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

import slash.navigation.routes.impl.RouteModel;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Acts as a {@link UndoableEdit} for renaming a {@link RouteModel} of a {@link UndoCatalogModel}.
 *
 * @author Christian Pesch
 */

class RenameRoute extends AbstractUndoableEdit {
    private UndoCatalogModel catalogModel;
    private RouteModel route;
    private String oldName, newName;

    public RenameRoute(UndoCatalogModel catalogModel, RouteModel route, String oldName, String newName) {
        this.catalogModel = catalogModel;
        this.route = route;
        this.oldName = oldName;
        this.newName = newName;
    }

    public String getUndoPresentationName() {
        return "rename-route-undo";
    }

    public String getRedoPresentationName() {
        return "rename-route-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        catalogModel.renameRoute(route, oldName, null, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        catalogModel.renameRoute(route, newName, null, false);
    }
}
