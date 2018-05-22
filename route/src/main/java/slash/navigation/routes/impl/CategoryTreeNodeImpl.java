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

import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import static java.util.Arrays.sort;

/**
 * Acts as a {@link TreeNode} for a {@link Category}.
 *
 * @author Christian Pesch
 */

public class CategoryTreeNodeImpl extends DefaultMutableTreeNode implements CategoryTreeNode {
    private static final Logger log = Logger.getLogger(CategoryTreeNodeImpl.class.getName());
    private static final CategoryComparator categoryComparator = new CategoryComparator();

    private boolean localRoot, remoteRoot;

    public CategoryTreeNodeImpl(Category category) {
        this(category, false, false);
    }

    public CategoryTreeNodeImpl(Category category, boolean localRoot, boolean remoteRoot) {
        super(category);
        this.localRoot = localRoot;
        this.remoteRoot = remoteRoot;
    }

    public int getChildCount() {
        ensureInited();
        return super.getChildCount();
    }

    public TreeNode getChildAt(int idx) {
        ensureInited();
        return super.getChildAt(idx);
    }

    public int getIndex(TreeNode aNode) {
        ensureInited();
        return super.getIndex(aNode);
    }

    @SuppressWarnings("unchecked")
    public Enumeration<TreeNode> children() {
        ensureInited();
        return super.children();
    }

    public Category getCategory() {
        return (Category) getUserObject();
    }

    private void ensureInited() {
        if (children == null) {
            try {
                List<Category> categories = getCategory().getCategories();
                Category[] categoriesArray = categories.toArray(new Category[0]);
                sort(categoriesArray, categoryComparator);

                // make sure there are always children even if insert() is never called
                if (children == null) {
                    children = new Vector<TreeNode>();
                }

                for (Category child : categoriesArray) {
                    insert(new CategoryTreeNodeImpl(child), children == null ? 0 : getChildCount());
                }
            } catch (Exception e) {
                log.severe("Cannot get child categories: " + e);
            }
        }
    }

    public boolean isLocalRoot() {
        return localRoot;
    }

    public boolean isLocal() {
        return isLocalRoot() || getParent() != null && ((CategoryTreeNode) getParent()).isLocal();
    }

    public boolean isRemoteRoot() {
        return remoteRoot;
    }

    public boolean isRemote() {
        return isRemoteRoot() || getParent() != null && ((CategoryTreeNode) getParent()).isRemote();
    }

    public String getName() {
        try {
            return getCategory().getName();
        } catch (Exception e) {
            log.severe("Cannot get name: " + e);
            return "?";
        }
    }

    public List<Route> getRoutes() {
        try {
            return getCategory().getRoutes();
        } catch (Exception e) {
            log.severe("Cannot get routes: " + e);
            return null;
        }
    }
}
