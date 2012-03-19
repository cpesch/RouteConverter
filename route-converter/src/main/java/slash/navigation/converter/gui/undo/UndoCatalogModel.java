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
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.converter.gui.helper.RouteServiceOperator;
import slash.navigation.converter.gui.models.CatalogModel;
import slash.navigation.converter.gui.models.CatalogModelImpl;
import slash.navigation.gui.UndoManager;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.List;

import static slash.navigation.converter.gui.helper.JTreeHelper.asNames;
import static slash.navigation.converter.gui.helper.JTreeHelper.asParents;

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

    // TreeModel

    public Object getRoot() {
        return delegate.getRoot();
    }

    public Object getChild(Object parent, int index) {
        return delegate.getChild(parent, index);
    }

    public int getChildCount(Object parent) {
        return delegate.getChildCount(parent);
    }

    public boolean isLeaf(Object node) {
        return delegate.isLeaf(node);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        delegate.valueForPathChanged(path, newValue);
    }

    public int getIndexOfChild(Object parent, Object child) {
        return delegate.getIndexOfChild(parent, child);
    }

    public void addTreeModelListener(TreeModelListener l) {
        delegate.addTreeModelListener(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        delegate.removeTreeModelListener(l);
    }

    // Undoable operations

    public void add(List<CategoryTreeNode> parents, List<String> names) {
        add(parents, names, true);
    }
    
    void add(List<CategoryTreeNode> categories, List<String> names, boolean trackUndo) {
        delegate.add(categories, names);
        if (trackUndo)
            undoManager.addEdit(new AddCategories(this, categories, names));
    }

    public void rename(CategoryTreeNode category, String name) {
        rename(category, name, true);
    }

    void rename(CategoryTreeNode category, String newName, boolean trackUndo) {
        String oldName = category.getName();
        delegate.rename(category, newName);
        if (trackUndo)
            undoManager.addEdit(new RenameCategory(this, category, oldName, newName));
    }

    public void move(List<CategoryTreeNode> categories, CategoryTreeNode parent) {
        move(categories, asParents(parent, categories.size()));
    }

    public void move(List<CategoryTreeNode> categories, List<CategoryTreeNode> parents) {
        move(categories, parents, true);
    }
    
    void move(List<CategoryTreeNode> categories, List<CategoryTreeNode> parents, boolean trackUndo) {
        List<CategoryTreeNode> oldParents = asParents(categories);
        delegate.move(categories, parents);
        if (trackUndo)
            undoManager.addEdit(new MoveCategories(this, categories, oldParents, parents));
    }

    public void remove(List<CategoryTreeNode> categories) {
        remove(asParents(categories), asNames(categories));
    }

    public void remove(List<CategoryTreeNode> parents, List<String> names) {
        remove(parents, names, true);
    }

    void remove(List<CategoryTreeNode> categories, List<String> names, boolean trackUndo) {
        delegate.remove(categories, names);
        if (trackUndo)
            undoManager.addEdit(new RemoveCategories(this, categories, names));
    }
}
