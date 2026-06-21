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

import slash.navigation.routes.Catalog;
import slash.navigation.routes.impl.CategoryTreeModel;
import slash.navigation.routes.impl.CategoryTreeNode;
import slash.navigation.routes.impl.RouteModel;
import slash.navigation.routes.impl.RoutesTableModel;

import javax.swing.tree.TreeModel;
import java.io.File;
import java.util.List;

/**
 * Acts as a {@link TreeModel} for the categories and routes of a {@link Catalog}.
 *
 * @author Christian Pesch
 */

public interface CatalogModel {
    CategoryTreeModel getCategoryTreeModel();
    RoutesTableModel getRoutesTableModel();

    void setCurrentCategory(CategoryTreeNode category);

    void addCategories(List<CategoryTreeNode> parents, List<String> names, Runnable invokeLaterRunnable);
    void renameCategory(CategoryTreeNode category, String name);
    void moveCategories(List<CategoryTreeNode> categories, CategoryTreeNode parent, Runnable invokeLaterRunnable);
    void moveCategories(List<CategoryTreeNode> categories, List<CategoryTreeNode> parents, Runnable invokeLaterRunnable);
    void deleteCategories(List<CategoryTreeNode> categories, Runnable invokeLaterRunnable);
    void deleteCategories(List<CategoryTreeNode> parents, List<String> names, Runnable invokeLaterRunnable);

    void addRoute(CategoryTreeNode category, String description, File file, String url, AddRouteCallback callback);
    void renameRoute(RouteModel route, String name, Runnable invokeLaterRunnable);
    void moveRoutes(List<RouteModel> routes, CategoryTreeNode parent, Runnable invokeLaterRunnable);
    void moveRoutes(List<RouteModel> routes, List<CategoryTreeNode> parents, Runnable invokeLaterRunnable);
    void deleteRoutes(List<RouteModel> routes);
}
