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
    public static String toString(TreePath treePath) {
        StringBuilder buffer = new StringBuilder();
        for (Object pathElement : treePath.getPath()) {
            CategoryTreeNode treeNode = (CategoryTreeNode) pathElement;
            String nodeName = treeNode.getName();
            if (nodeName.length() > 0)
                buffer.append("/").append(nodeName);
        }
        return buffer.toString();
    }

    public static TreePath fromString(CategoryTreeNode root, String path) {
        List<CategoryTreeNode> result = new ArrayList<CategoryTreeNode>();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        CategoryTreeNode current = root;
        result.add(root);
        while (tokenizer.hasMoreTokens()) {
            String pathElement = tokenizer.nextToken();
            CategoryTreeNode next = current.getSubCategory(pathElement);
            if (next != null) {
                result.add(next);
                current = next;
            }
        }
        return new TreePath(result.toArray());
    }
}

