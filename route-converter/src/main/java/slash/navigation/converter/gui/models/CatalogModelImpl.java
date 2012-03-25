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
import slash.navigation.catalog.model.CategoryTreeModel;
import slash.navigation.catalog.model.CategoryTreeNode;
import slash.navigation.catalog.model.RouteModel;
import slash.navigation.catalog.model.RoutesTableModel;
import slash.navigation.converter.gui.helper.RouteServiceOperator;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.util.List;

import static slash.navigation.converter.gui.helper.JTreeHelper.asNames;
import static slash.navigation.converter.gui.helper.JTreeHelper.asParents;

/**
 * Acts as a {@link TreeModel} for the categories and routes of a {@link Catalog}.
 *
 * @author Christian Pesch
 */

public class CatalogModelImpl implements CatalogModel {
    private final CategoryTreeModel categoryTreeModel;
    private final RoutesTableModel routesTableModel = new RoutesTableModel();
    private final RouteServiceOperator operator;

    public CatalogModelImpl(CategoryTreeNode root, RouteServiceOperator operator) {
        categoryTreeModel = new CategoryTreeModel(root);
        this.operator = operator;
    }

    public CategoryTreeModel getCategoryTreeModel() {
        return categoryTreeModel;
    }

    public RoutesTableModel getRoutesTableModel() {
        return routesTableModel;
    }

    public void add(final List<CategoryTreeNode> parents, final List<String> names, final Runnable invokeLaterRunnable) {
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
                            categoryTreeModel.nodeStructureChanged(parent);
                        }
                        if (invokeLaterRunnable != null)
                            invokeLaterRunnable.run();
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
                category.getCategory().update(((CategoryTreeNode) category.getParent()).getCategory(), name);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        category.clearChildren();
                        categoryTreeModel.nodeChanged(category);
                    }
                });
            }
        });
    }

    public void move(List<CategoryTreeNode> categories, CategoryTreeNode parent) {
        move(categories, asParents(parent, categories.size()));
    }

    public void move(final List<CategoryTreeNode> categories, final List<CategoryTreeNode> parents) {
        operator.executeOperation(new RouteServiceOperator.NewOperation() {
            public String getName() {
                return "MoveCategories";
            }

            public void run() throws IOException {
                for (int i = 0; i < categories.size(); i++) {
                    CategoryTreeNode category = categories.get(i);
                    CategoryTreeNode parent = parents.get(i);

                    if (category.isLocal() && parent.isRemote())
                        throw new IOException("cannot move local category " + category.getName() + " to remote parent " + parent.getName());
                    if (category.isRemote() && parent.isLocal())
                        throw new IOException("cannot move remote category " + category.getName() + " to local parent " + parent.getName());

                    category.getCategory().update(parent.getCategory(), category.getCategory().getName());
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < categories.size(); i++) {
                            CategoryTreeNode category = categories.get(i);
                            CategoryTreeNode parent = parents.get(i);
                            categoryTreeModel.removeNodeFromParent(category);
                            categoryTreeModel.insertNodeInto(category, parent, 0);
                        }
                    }
                });
            }
        });
    }

    public void remove(List<CategoryTreeNode> categories) {
        remove(asParents(categories), asNames(categories));
    }

    public void remove(final List<CategoryTreeNode> parents, final List<String> names) {
        operator.executeOperation(new RouteServiceOperator.NewOperation() {
            public String getName() {
                return "RemoveCategories";
            }

            public void run() throws IOException {
                for (int i = 0; i < parents.size(); i++) {
                    CategoryTreeNode category = categoryTreeModel.getChild(parents.get(i), names.get(i));
                    category.getCategory().delete();
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < parents.size(); i++) {
                            CategoryTreeNode category = categoryTreeModel.getChild(parents.get(i), names.get(i));
                            categoryTreeModel.removeNodeFromParent(category);
                        }
                    }
                });
            }
        });
    }

    public void rename(final RouteModel route, final String name) {
        operator.executeOperation(new RouteServiceOperator.NewOperation() {
            public String getName() {
                return "RenameRoute";
            }

            public void run() throws IOException {
                route.getRoute().update(route.getCategory().getCategory().getUrl(), name);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        routesTableModel.updateRoute(route);
                    }
                });
            }
        });
    }
}
