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

import slash.navigation.catalog.model.CategoryTreeNode;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.util.List;

/**
 * Acts as a {@link UndoableEdit} for removing {@link CategoryTreeNode}s of a {@link UndoCatalogModel}.
 *
 * @author Christian Pesch
 */

class RemoveCategories extends AbstractUndoableEdit {
    private final UndoCatalogModel catalogModel;
    private final List<CategoryTreeNode> categories;
    private final List<String> names;

    public RemoveCategories(UndoCatalogModel catalogModel, List<CategoryTreeNode> categories, List<String> names) {
        this.catalogModel = catalogModel;
        this.categories = categories;
        this.names = names;
    }

    public String getUndoPresentationName() {
        return "remove-category-undo";
    }

    public String getRedoPresentationName() {
        return "remove-category-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        catalogModel.add(categories, names, null, false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        catalogModel.remove(categories, names, false);
    }
}
