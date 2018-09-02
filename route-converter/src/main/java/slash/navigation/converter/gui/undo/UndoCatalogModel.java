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

import slash.navigation.converter.gui.helpers.RouteServiceOperator;
import slash.navigation.converter.gui.models.AddRouteCallback;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.converter.gui.models.CatalogModelImpl;
import slash.navigation.gui.undo.UndoManager;
import slash.navigation.routes.Catalog;
import slash.navigation.routes.impl.CategoryTreeModel;
import slash.navigation.routes.impl.CategoryTreeNode;
import slash.navigation.routes.impl.RouteModel;
import slash.navigation.routes.impl.RoutesTableModel;

import javax.swing.tree.TreeModel;
import java.io.File;
import java.util.List;

import static slash.navigation.converter.gui.helpers.RouteModelHelper.asNames;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.asParents;
import static slash.navigation.converter.gui.helpers.RouteModelHelper.asParentsFromRoutes;

/**
 * Acts as a {@link TreeModel} for the categories and routes of a {@link Catalog}.
 *
 * @author Christian Pesch
 */

public class UndoCatalogModel implements CatalogModel {
    private final CatalogModelImpl delegate;
    private final UndoManager undoManager;

    public UndoCatalogModel(UndoManager undoManager, CategoryTreeNode root, RouteServiceOperator operator) {
        this.delegate = new CatalogModelImpl(root, operator);
        this.undoManager = undoManager;
    }

    public CategoryTreeModel getCategoryTreeModel() {
        return delegate.getCategoryTreeModel();
    }

    public RoutesTableModel getRoutesTableModel() {
        return delegate.getRoutesTableModel();
    }

    public void setCurrentCategory(CategoryTreeNode category) {
        delegate.setCurrentCategory(category);
    }

    public void addCategories(List<CategoryTreeNode> parents, List<String> names, Runnable invokeLaterRunnable) {
        addCategories(parents, names, invokeLaterRunnable, true);
    }
    
    void addCategories(List<CategoryTreeNode> categories, List<String> names, Runnable invokeLaterRunnable, boolean trackUndo) {
        delegate.addCategories(categories, names, invokeLaterRunnable);
        if (trackUndo)
            undoManager.addEdit(new AddCategories(this, categories, names));
    }

    public void renameCategory(CategoryTreeNode category, String name) {
        renameCategory(category, name, true);
    }

    void renameCategory(CategoryTreeNode category, String newName, boolean trackUndo) {
        String oldName = category.getName();
        delegate.renameCategory(category, newName);
        if (trackUndo)
            undoManager.addEdit(new RenameCategory(this, category, oldName, newName));
    }

    public void moveCategories(List<CategoryTreeNode> categories, CategoryTreeNode parent, Runnable invokeLaterRunnable) {
        moveCategories(categories, asParents(parent, categories.size()), invokeLaterRunnable);
    }

    public void moveCategories(List<CategoryTreeNode> categories, List<CategoryTreeNode> parents, Runnable invokeLaterRunnable) {
        moveCategories(categories, parents, invokeLaterRunnable, true);
    }

    void moveCategories(List<CategoryTreeNode> categories, List<CategoryTreeNode> parents, Runnable invokeLaterRunnable, boolean trackUndo) {
        List<CategoryTreeNode> oldParents = asParents(categories);
        delegate.moveCategories(categories, parents, invokeLaterRunnable);
        if (trackUndo)
            undoManager.addEdit(new MoveCategories(this, categories, oldParents, parents));
    }

    public void deleteCategories(List<CategoryTreeNode> categories, Runnable invokeLaterRunnable) {
        deleteCategories(asParents(categories), asNames(categories), invokeLaterRunnable);
    }

    public void deleteCategories(List<CategoryTreeNode> parents, List<String> names, Runnable invokeLaterRunnable) {
        deleteCategories(parents, names, invokeLaterRunnable, true);
    }

    void deleteCategories(List<CategoryTreeNode> categories, List<String> names, Runnable invokeLaterRunnable, boolean trackUndo) {
        delegate.deleteCategories(categories, names, invokeLaterRunnable);
        if (trackUndo)
            undoManager.addEdit(new DeleteCategories(this, categories, names));
    }

    public void addRoute(CategoryTreeNode category, String description, File file, String url, AddRouteCallback callback) {
        addRoute(category, description, file, url, callback, true);
    }

    void addRoute(CategoryTreeNode category, String description, File file, String url, AddRouteCallback callback, boolean trackUndo) {
        delegate.addRoute(category, description, file, url, callback);
        if (trackUndo)
            undoManager.addEdit(new AddRoute(this, category, description, file, url, callback));
    }

    public void renameRoute(RouteModel route, String name, Runnable invokeLaterRunnable) {
        renameRoute(route, name, invokeLaterRunnable, true);
    }

    void renameRoute(RouteModel route, String newName, Runnable invokeLaterRunnable, boolean trackUndo) {
        String oldName = route.getName();
        delegate.renameRoute(route, newName, invokeLaterRunnable);
        if (trackUndo)
            undoManager.addEdit(new RenameRoute(this, route, oldName, newName));

    }

    public void moveRoutes(List<RouteModel> routes, CategoryTreeNode parent, Runnable invokeLaterRunnable) {
        moveRoutes(routes, asParents(parent, routes.size()), invokeLaterRunnable);
    }

    public void moveRoutes(List<RouteModel> routes, List<CategoryTreeNode> parents, Runnable invokeLaterRunnable) {
        moveRoutes(routes, parents, invokeLaterRunnable, true);
    }

    void moveRoutes(List<RouteModel> routes, List<CategoryTreeNode> parents, Runnable invokeLaterRunnable, boolean trackUndo) {
        List<CategoryTreeNode> oldParents = asParentsFromRoutes(routes);
        delegate.moveRoutes(routes, parents, invokeLaterRunnable);
        if (trackUndo)
            undoManager.addEdit(new MoveRoutes(this, routes, oldParents, parents));
    }

    public void deleteRoutes(List<RouteModel> routes) {
        deleteRoutes(routes, true);
    }

    void deleteRoutes(List<RouteModel> routes, boolean trackUndo) {
        delegate.deleteRoutes(routes);
        if (trackUndo)
            undoManager.addEdit(new DeleteRoutes(this, routes));
    }
}
