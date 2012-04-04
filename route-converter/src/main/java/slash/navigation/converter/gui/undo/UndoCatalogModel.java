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

import slash.navigation.catalog.domain.Catalog;
import slash.navigation.catalog.model.CategoryTreeModel;
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.catalog.model.RouteModel;
import slash.navigation.catalog.model.RoutesTableModel;
import slash.navigation.converter.gui.helper.RouteServiceOperator;
import slash.navigation.converter.gui.models.AddRouteCallback;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.converter.gui.models.CatalogModelImpl;
import slash.navigation.gui.UndoManager;

import javax.swing.tree.TreeModel;
import java.io.File;
import java.util.List;

import static slash.navigation.converter.gui.helper.JTreeHelper.asNames;
import static slash.navigation.converter.gui.helper.JTreeHelper.asParents;
import static slash.navigation.converter.gui.helper.JTreeHelper.asParentsFromRoutes;

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

    public void removeCategories(List<CategoryTreeNode> categories, Runnable invokeLaterRunnable) {
        removeCategories(asParents(categories), asNames(categories), invokeLaterRunnable);
    }

    public void removeCategories(List<CategoryTreeNode> parents, List<String> names, Runnable invokeLaterRunnable) {
        removeCategories(parents, names, invokeLaterRunnable, true);
    }

    void removeCategories(List<CategoryTreeNode> categories, List<String> names, Runnable invokeLaterRunnable, boolean trackUndo) {
        delegate.removeCategories(categories, names, invokeLaterRunnable);
        if (trackUndo)
            undoManager.addEdit(new RemoveCategories(this, categories, names));
    }

    public void addRoute(CategoryTreeNode category, String description, File file, String url, AddRouteCallback callback) {
        addRoute(category, description, file, url, callback, true);
    }

    void addRoute(CategoryTreeNode category, String description, File file, String url, AddRouteCallback callback, boolean trackUndo) {
        delegate.addRoute(category, description, file, url, callback);
        if (trackUndo)
            undoManager.addEdit(new AddRoute(this, category, description, file, url, callback));
    }

    public void renameRoute(RouteModel route, String name) {
        renameRoute(route, name, true);
    }

    void renameRoute(RouteModel route, String newName, boolean trackUndo) {
        String oldName = route.getName();
        delegate.renameRoute(route, newName);
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

    public void removeRoutes(List<RouteModel> routes) {
        removeRoutes(routes, true);
    }

    void removeRoutes(List<RouteModel> routes, boolean trackUndo) {
        delegate.removeRoutes(routes);
        if (trackUndo)
            undoManager.addEdit(new RemoveRoutes(this, routes));
    }
}
