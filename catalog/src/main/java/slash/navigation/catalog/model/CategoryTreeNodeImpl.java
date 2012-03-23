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
    private static final RouteComparator routeComparator = new RouteComparator();
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

    public Enumeration children() {
        ensureInited();
        return super.children();
    }

    public void clearChildren() {
        children = null;
    }

    public Category getCategory() {
        return (Category) getUserObject();
    }

    private void ensureInited() {
        if (children == null) {
            try {
                List<Category> categories = getCategory().getCategories();
                Category[] categoriesArray = categories.toArray(new Category[categories.size()]);
                sort(categoriesArray, categoryComparator);

                // make sure there are always children even if insert() is never called
                if (children == null) {
                    children = new Vector();
                }

                for (Category child : categoriesArray) {
                    insert(new CategoryTreeNodeImpl(child), children == null ? 0 : getChildCount());
                }
            } catch (Exception e) {
                log.severe("Cannot get child categories: " + e.getMessage());
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
            log.severe("Cannot get name: " + e.getMessage());
            return "?";
        }
    }

    private RoutesListModel routesListModel;

    public RoutesListModel getRoutesListModel() {
        if (routesListModel == null) {
            try {
                List<Route> routes = getCategory().getRoutes();
                Route[] routesArray = routes.toArray(new Route[routes.size()]);
                sort(routesArray, routeComparator);
                List<RouteModel> routeModels = new ArrayList<RouteModel>();
                for (Route route : routes)
                    routeModels.add(new RouteModel(this, route));
                routesListModel = new RoutesListModel(routeModels);
            } catch (Exception e) {
                log.severe("Cannot get routes: " + e.getMessage());
            }
        }
        return routesListModel;
    }

    public Route addRoute(String description, File file) throws IOException {
        Route route = getCategory().createRoute(description, file);
        getRoutesListModel().addRoute(new RouteModel(this, route));
        return route;
    }

    public Route addRoute(String description, String fileUrl) throws IOException {
        Route route = getCategory().createRoute(description, fileUrl);
        getRoutesListModel().addRoute(new RouteModel(this, route));
        return route;
    }

    public void moveRoute(Route route, CategoryTreeNode target) throws IOException {
        route.update(target.getCategory().getUrl(), route.getDescription());
        target.getRoutesListModel().addRoute(new RouteModel(this, route));
        getRoutesListModel().deleteRoute(new RouteModel(this, route));
    }

    public void deleteRoute(Route route) throws IOException {
        route.delete();
        getRoutesListModel().deleteRoute(new RouteModel(this, route));
    }
}
