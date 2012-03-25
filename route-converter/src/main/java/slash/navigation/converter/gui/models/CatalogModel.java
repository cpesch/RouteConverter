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

import javax.swing.tree.TreeModel;
import java.util.List;

/**
 * Acts as a {@link TreeModel} for the categories and routes of a {@link Catalog}.
 *
 * @author Christian Pesch
 */

public interface CatalogModel {
    CategoryTreeModel getCategoryTreeModel();
    RoutesTableModel getRoutesTableModel();
    
    void add(List<CategoryTreeNode> parents, List<String> names, Runnable invokeLaterRunnable);
    void rename(CategoryTreeNode category, String name);
    void move(List<CategoryTreeNode> categories, CategoryTreeNode parent);
    void move(List<CategoryTreeNode> categories, List<CategoryTreeNode> parents);
    void remove(List<CategoryTreeNode> categories);
    void remove(List<CategoryTreeNode> parents, List<String> names);

    void rename(RouteModel route, String name);
}
