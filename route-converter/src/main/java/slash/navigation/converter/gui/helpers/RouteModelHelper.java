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

package slash.navigation.converter.gui.helpers;

import slash.navigation.routes.impl.CategoryTreeModel;
import slash.navigation.routes.impl.CategoryTreeNode;
import slash.navigation.routes.impl.RouteModel;
import slash.navigation.routes.impl.RoutesTableModel;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;

/**
 * A helper for simplified {@link RouteModel} operations.
 *
 * @author Christian Pesch
 */

public class RouteModelHelper {
    public static List<RouteModel> getSelectedRouteModels(JTable table) {
        int[] selectedRows = table.getSelectedRows();
        List<RouteModel> routeModels = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int row = table.convertRowIndexToView(selectedRow);
            Object value = table.getModel().getValueAt(row, 1);
            if (value instanceof RouteModel)
                routeModels.add((RouteModel) value);
        }
        return routeModels;
    }

    public static CategoryTreeNode getSelectedCategoryTreeNode(JTree tree) {
        TreePath treePath = tree.getSelectionPath();
        // if there is no selected root take the local root
        Object value = treePath != null ?
                treePath.getLastPathComponent() :
                tree.getModel().getChild(tree.getModel().getRoot(), 0);
        if (!(value instanceof CategoryTreeNode))
            return null;
        return (CategoryTreeNode) value;
    }

    public static void selectRoute(JTable table, RouteModel route) {
        // search for RouteModel with same Route (Category might be different due to move)
        RoutesTableModel model = (RoutesTableModel) table.getModel();
        for(int i = 0; i < model.getRowCount(); i++) {
            if(model.getRoute(i).getRoute().equals(route.getRoute())) {
                scrollToPosition(table, i);
                table.getSelectionModel().addSelectionInterval(i, i);
                break;
            }
        }
    }

    public static List<CategoryTreeNode> getSelectedCategoryTreeNodes(JTree tree) {
        TreePath[] treePaths = tree.getSelectionPaths();
        List<CategoryTreeNode> treeNodes = new ArrayList<>();
        if (treePaths != null) {
            for (TreePath treePath : treePaths) {
                Object treeNode = treePath.getLastPathComponent();
                if (!(treeNode instanceof CategoryTreeNode))
                    continue;
                treeNodes.add((CategoryTreeNode) treeNode);
            }
        }
        return treeNodes;
    }

    public static void selectCategory(JTree tree, CategoryTreeNode category) {
        TreePath treePath = new TreePath(((CategoryTreeModel)tree.getModel()).getPathToRoot(category));
        selectCategoryTreePath(tree, treePath);
    }

    public static void selectCategoryTreePath(JTree tree, TreePath treePath) {
        tree.expandPath(treePath);
        tree.scrollPathToVisible(treePath);
        tree.getSelectionModel().setSelectionPath(treePath);
    }

    public static List<String> asNames(List<CategoryTreeNode> categories) {
        List<String> names = new ArrayList<>(categories.size());
        for (CategoryTreeNode categoryTreeNode : categories) {
            names.add(categoryTreeNode.getName());
        }
        return names;
    }

    public static List<CategoryTreeNode> asParents(List<CategoryTreeNode> categories) {
        List<CategoryTreeNode> parents = new ArrayList<>(categories.size());
        for (CategoryTreeNode categoryTreeNode : categories) {
            parents.add((CategoryTreeNode) categoryTreeNode.getParent());
        }
        return parents;
    }

    public static List<CategoryTreeNode> asParentsFromRoutes(List<RouteModel> routes) {
        List<CategoryTreeNode> parents = new ArrayList<>(routes.size());
        for (RouteModel routeModel : routes) {
            parents.add(routeModel.getCategory());
        }
        return parents;
    }

    public static List<CategoryTreeNode> asParents(CategoryTreeNode parent, int count) {
        List<CategoryTreeNode> parents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            parents.add(parent);
        }
        return parents;
    }
}