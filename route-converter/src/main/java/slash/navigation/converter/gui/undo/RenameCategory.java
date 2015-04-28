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

import slash.navigation.routes.impl.CategoryTreeNode;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * Acts as a {@link UndoableEdit} for renaming a {@link CategoryTreeNode} of a {@link UndoCatalogModel}.
 *
 * @author Christian Pesch
 */

class RenameCategory extends AbstractUndoableEdit {
    private UndoCatalogModel catalogModel;
    private CategoryTreeNode category;
    private String oldName, newName;
    
    public RenameCategory(UndoCatalogModel catalogModel, CategoryTreeNode category, String oldName, String newName) {
        this.catalogModel = catalogModel;
        this.category = category;
        this.oldName = oldName;
        this.newName = newName;
    }

    public String getUndoPresentationName() {
        return "rename-category-undo";
    }

    public String getRedoPresentationName() {
        return "rename-category-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        catalogModel.renameCategory(category, oldName, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        catalogModel.renameCategory(category, newName, false);
    }
}
