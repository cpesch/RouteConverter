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

import slash.navigation.routes.impl.CategoryTreeNode;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Helps to convert between {@link TreePath}s and {@link String}s.
 *
 * @author Christian Pesch
 */

public class TreePathStringConversion {
    private static final String LOCAL = "LOCAL:";
    private static final String REMOTE = "REMOTE:";

    public static String toString(TreePath treePath) {
        StringBuilder buffer = new StringBuilder();
        for (Object pathElement : treePath.getPath()) {
            CategoryTreeNode treeNode = (CategoryTreeNode) pathElement;
            String nodeName = treeNode.getName();
            if (treeNode.isLocalRoot())
                buffer.append(LOCAL);
            else if (treeNode.isRemoteRoot())
                buffer.append(REMOTE);
            else if (nodeName != null)
                buffer.append("/").append(nodeName);
        }
        return buffer.toString();
    }

    private static CategoryTreeNode getSubCategory(CategoryTreeNode node, String name) {
        for (int i = 0; i < node.getChildCount(); i++) {
            CategoryTreeNode child = (CategoryTreeNode) node.getChildAt(i);
            if (child.getName().equals(name) ||
                    child.isLocalRoot() && LOCAL.equals(name) ||
                    child.isRemoteRoot() && REMOTE.equals(name))
                return child;
        }
        return null;
    }

    public static TreePath fromString(CategoryTreeNode root, String path) {
        List<CategoryTreeNode> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        CategoryTreeNode current = root;
        result.add(root);
        while (tokenizer.hasMoreTokens()) {
            String pathElement = tokenizer.nextToken();
            CategoryTreeNode next = getSubCategory(current, pathElement);
            if (next != null) {
                result.add(next);
                current = next;
            }
        }
        return new TreePath(result.toArray());
    }
}

