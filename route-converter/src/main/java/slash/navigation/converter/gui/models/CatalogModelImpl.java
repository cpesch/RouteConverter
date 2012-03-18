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

package slash.navigation.converter.gui.models;

import slash.navigation.catalog.domain.Catalog;
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.converter.gui.helper.RouteServiceOperator;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.util.List;

/**
 * Acts as a {@link TreeModel} for the categories and routes of a {@link Catalog}.
 *
 * @author Christian Pesch
 */

public class CatalogModelImpl extends DefaultTreeModel implements CatalogModel {
    private final RouteServiceOperator operator;

    public CatalogModelImpl(CategoryTreeNode root, RouteServiceOperator operator) {
        super(root);
        root.setTreeModel(this);
        this.operator = operator;
    }

    public boolean isLeaf(Object node) {
        // this would go through the whole tree ((CategoryTreeNode) node).getChildCount() == 0;
        return false;
    }

    private CategoryTreeNode getChild(CategoryTreeNode parent, String name) {
        for(int i=0; i<getChildCount(parent); i++) {
            CategoryTreeNode category = (CategoryTreeNode) getChild(parent, i);
            if(category.getName().equals(name))
                return category;
        }
        return null;
    }

    // Undoable operations

    public void add(final List<CategoryTreeNode> parents, final List<String> names) {
        operator.executeOperation(new RouteServiceOperator.NewOperation() {
            public String getName() {
                return "AddCategories";
            }

            public void run() throws IOException {
                for (int i = 0; i < parents.size(); i++) {
                    CategoryTreeNode category = parents.get(i);
                    category.getCategory().create(names.get(i));
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (CategoryTreeNode parent : parents) {
                            parent.clearChildren();
                        }
                        for (CategoryTreeNode parent : parents) {
                            nodeStructureChanged(parent);
                        }
                    }
                });
            }
        });
    }

    public void rename(final CategoryTreeNode category, final String name) {
        operator.executeOperation(new RouteServiceOperator.NewOperation() {
            public String getName() {
                return "RenameCategory";
            }

            public void run() throws IOException {
                category.getCategory().update(null, name);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        category.clearChildren();
                        nodeChanged(category);
                    }
                });
            }
        });
    }
    
    public void remove(final List<CategoryTreeNode> parents, final List<String> names) {
        operator.executeOperation(new RouteServiceOperator.NewOperation() {
            public String getName() {
                return "RemoveCategories";
            }

            public void run() throws IOException {
                for (int i = 0; i < parents.size(); i++) {
                    CategoryTreeNode category = getChild(parents.get(i), names.get(i));
                    category.getCategory().delete();
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < parents.size(); i++) {
                            CategoryTreeNode category = getChild(parents.get(i), names.get(i));
                            removeNodeFromParent(category);
                        }
                    }
                });
            }
        });
    }
}
