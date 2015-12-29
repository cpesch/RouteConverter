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
import slash.navigation.routes.impl.RouteModel;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static slash.common.io.Files.toFile;

/**
 * Acts as a {@link UndoableEdit} for deleting {@link RouteModel}s of a {@link UndoCatalogModel}.
 *
 * @author Christian Pesch
 */

class DeleteRoutes extends AbstractUndoableEdit {
    private final UndoCatalogModel catalogModel;
    private final List<RouteModel> routes;
    private final List<CategoryTreeNode> categories = new ArrayList<>();
    private final List<String> descriptions = new ArrayList<>();
    private final List<String> files = new ArrayList<>();
    private final List<String> urls = new ArrayList<>();

    public DeleteRoutes(UndoCatalogModel catalogModel, List<RouteModel> routes) {
        this.catalogModel = catalogModel;
        this.routes = routes;
        for (RouteModel route : routes) {
            categories.add(route.getCategory());
            descriptions.add(route.getDescription() != null ? route.getDescription() : route.getName());
            try {
                files.add(route.getRoute().getUrl());
            } catch (IOException e) {
                files.add(null);
            }
            urls.add(route.getRoute().getHref());
        }
    }

    public String getUndoPresentationName() {
        return "delete-route-undo";
    }

    public String getRedoPresentationName() {
        return "delete-route-redo";
    }

    public void undo() throws CannotUndoException {
        super.undo();
        for (int i = 0; i < categories.size(); i++) {
            String file = files.get(i);
            try {
                catalogModel.addRoute(categories.get(i), descriptions.get(i), toFile(new URL(file)), urls.get(i), new AddRouteCallback(), false);
            } catch (MalformedURLException e) {
                throw new IllegalStateException(format("Cannot create URL for %s", file));
            }
        }
    }

    public void redo() throws CannotRedoException {
        super.redo();
        catalogModel.deleteRoutes(routes, false);
    }
}
