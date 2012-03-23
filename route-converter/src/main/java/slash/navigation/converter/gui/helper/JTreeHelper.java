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

package slash.navigation.converter.gui.helper;

import slash.navigation.catalog.model.CategoryTreeNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper for simplified {@link JTree} operations.
 *
 * @author Christian Pesch
 */

public class JTreeHelper {
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

    public static List<CategoryTreeNode> getSelectedCategoryTreeNodes(JTree tree) {
        TreePath[] treePaths = tree.getSelectionPaths();
        List<CategoryTreeNode> treeNodes = new ArrayList<CategoryTreeNode>();
        for (TreePath treePath : treePaths) {
            Object treeNode = treePath.getLastPathComponent();
            if (!(treeNode instanceof CategoryTreeNode))
                continue;
            treeNodes.add((CategoryTreeNode) treeNode);
        }
        return treeNodes;
    }

    public static List<String> asNames(List<CategoryTreeNode> categories) {
        List<String> names = new ArrayList<String>(categories.size());
        for (CategoryTreeNode categoryTreeNode : categories) {
            names.add(categoryTreeNode.getName());
        }
        return names;
    }

    public static List<CategoryTreeNode> asParents(List<CategoryTreeNode> categories) {
        List<CategoryTreeNode> parents = new ArrayList<CategoryTreeNode>(categories.size());
        for (CategoryTreeNode categoryTreeNode : categories) {
            parents.add((CategoryTreeNode) categoryTreeNode.getParent());
        }
        return parents;
    }

    public static List<CategoryTreeNode> asParents(CategoryTreeNode parent, int count) {
        List<CategoryTreeNode> parents = new ArrayList<CategoryTreeNode>(count);
        for (int i = 0; i < count; i++) {
            parents.add(parent);
        }
        return parents;
    }
}