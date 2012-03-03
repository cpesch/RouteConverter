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

package slash.navigation.catalog.model;

import slash.navigation.catalog.domain.Category;
import slash.navigation.catalog.domain.Route;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Acts as a {@link TreeNode} for a {@link Category}.
 *
 * @author Christian Pesch
 */

public class CategoryTreeNode extends DefaultMutableTreeNode {
    private static final Logger log = Logger.getLogger(CategoryTreeNode.class.getName());
    private static final RouteComparator routeComparator = new RouteComparator();
    private static final CategoryComparator categoryComparator = new CategoryComparator();

    public CategoryTreeNode(Category category) {
        super(category);
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

    public Enumeration children() {
        ensureInited();
        return super.children();
    }

    Category getCategory() {
        return (Category) getUserObject();
    }

    private void ensureInited() {
        if (children == null) {
            try {
                List<Category> categories = getCategory().getSubCategories();
                Category[] categoriesArray = categories.toArray(new Category[categories.size()]);
                Arrays.sort(categoriesArray, categoryComparator);
                for (Category child : categoriesArray) {
                    insert(new CategoryTreeNode(child), children == null ? 0 : getChildCount());
                }
            } catch (Exception e) {
                log.severe("Cannot get child categories: " + e.getMessage());
            }
        }
    }

    private CategoryTreeModel treeModel;

    CategoryTreeModel getTreeModel() {
        if (treeModel == null) {
            CategoryTreeNode parent = (CategoryTreeNode) getParent();
            if (parent != null)
                treeModel = parent.getTreeModel();
        }
        return treeModel;
    }

    public void setTreeModel(CategoryTreeModel treeModel) {
        this.treeModel = treeModel;
    }

    public String getName() {
        try {
            return getCategory().getName();
        } catch (Exception e) {
            log.severe("Cannot get name: " + e.getMessage());
            return "?";
        }
    }

    public CategoryTreeNode getSubCategory(String name) {
        for (int i = 0; i < getChildCount(); i++) {
            CategoryTreeNode child = (CategoryTreeNode) getChildAt(i);
            if (child.getName().equals(name))
                return child;
        }
        return null;
    }

    private RoutesListModel routesListModel;

    public RoutesListModel getRoutesListModel() {
        if (routesListModel == null) {
            try {
                List<Route> routes = getCategory().getRoutes();
                Route[] routesArray = routes.toArray(new Route[routes.size()]);
                Arrays.sort(routesArray, routeComparator);
                routes = new ArrayList<Route>(Arrays.asList(routesArray));
                routesListModel = new RoutesListModel(routes);
            } catch (Exception e) {
                log.severe("Cannot get routes: " + e.getMessage());
            }
        }
        return routesListModel;
    }

    public CategoryTreeNode addSubCategory(String name) throws IOException {
        Category subCategory = getCategory().addSubCategory(name);
        ensureInited();
        CategoryTreeNode treeNode = new CategoryTreeNode(subCategory);
        getTreeModel().insertNodeInto(treeNode, this, Math.max(children == null ? 0 : getChildCount() - 1, 0));
        return treeNode;
    }

    public void renameCategory(String name) throws IOException {
        getCategory().updateCategory(null, name);
        children = null;
        getTreeModel().nodeChanged(this);
        getTreeModel().nodeStructureChanged(this);
    }

    public void moveCategory(CategoryTreeNode parent) throws IOException {
        getCategory().updateCategory(parent.getCategory(), getCategory().getName());
        getTreeModel().removeNodeFromParent(this);
        getTreeModel().insertNodeInto(this, parent, Math.max(children == null ? 0 : getChildCount() - 1, 0));
    }

    public void delete() throws IOException {
        getCategory().delete();
        getTreeModel().removeNodeFromParent(this);
    }

    public Route addRoute(String description, File file) throws IOException {
        Route route = getCategory().addRoute(description, file);
        getRoutesListModel().addRoute(route);
        return route;
    }

    public Route addRoute(String description, String fileUrl) throws IOException {
        Route route = getCategory().addRoute(description, fileUrl);
        getRoutesListModel().addRoute(route);
        return route;
    }

    public void renameRoute(Route route, String description) throws IOException {
        getCategory().updateRoute(route, getCategory(), description);
        getRoutesListModel().updateRoute(route);
    }

    public void moveRoute(Route route, CategoryTreeNode target) throws IOException {
        getCategory().updateRoute(route, target.getCategory(), route.getDescription());
        target.getRoutesListModel().addRoute(route);
        getRoutesListModel().deleteRoute(route);
    }

    public void deleteRoute(Route route) throws IOException {
        getCategory().deleteRoute(route);
        getRoutesListModel().deleteRoute(route);
    }
}
