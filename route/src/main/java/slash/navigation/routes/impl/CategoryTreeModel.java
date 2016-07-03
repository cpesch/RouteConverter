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

package slash.navigation.routes.impl;

import slash.navigation.routes.Catalog;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

/**
 * Acts as a {@link TreeModel} for a {@link Catalog}.
 *
 * @author Christian Pesch
 */

public class CategoryTreeModel extends DefaultTreeModel {
    public CategoryTreeModel(CategoryTreeNode root) {
        super(root);
    }

    public boolean isLeaf(Object node) {
        // this would go through the whole tree ((CategoryTreeNode) node).getChildCount() == 0;
        return false;
    }

    public CategoryTreeNode getChild(CategoryTreeNode parent, String name) {
        for (int i = 0; i < getChildCount(parent); i++) {
            CategoryTreeNode category = (CategoryTreeNode) getChild(parent, i);
            if (category.getName().equals(name))
                return category;
        }
        return null;
    }
}
