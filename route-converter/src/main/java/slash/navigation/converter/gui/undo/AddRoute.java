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

import slash.navigation.converter.gui.models.AddRouteCallback;
import slash.navigation.routes.impl.CategoryTreeNode;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.io.File;
import java.net.URL;

import static java.util.Collections.singletonList;

/**
 * Acts as a {@link UndoableEdit} for adding a {@link File} or {@link URL} to a {@link CategoryTreeNode} of a {@link UndoCatalogModel}.
 *
 * @author Christian Pesch
 */

class AddRoute extends AbstractUndoableEdit {
    private final UndoCatalogModel catalogModel;
    private final CategoryTreeNode category;
    private final String description;
    private final File file;
    private final String url;
    private final AddRouteCallback callback;
    
    public AddRoute(UndoCatalogModel catalogModel, CategoryTreeNode category, String description, File file, String url, AddRouteCallback callback) {
        this.catalogModel = catalogModel;
        this.category = category;
        this.description = description;
        this.file = file;
        this.url = url;
        this.callback = callback;
    }

    public String getUndoPresentationName() {
        return "add-route-undo";
    }

    public String getRedoPresentationName() {
        return "add-route-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        catalogModel.deleteRoutes(singletonList(callback.getRoute()), false);
    }

    public void redo() throws CannotRedoException {
        super.redo();
        catalogModel.addRoute(category, description, file, url, callback, false);
    }
}
