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
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;

/**
 * A {@link CategoryTreeNode} that has a given set of {@link CategoryTreeNode} as its children.
 *
 * @author Christian Pesch
 */

public class RootTreeNode extends DefaultMutableTreeNode implements CategoryTreeNode {
    private DefaultTreeModel treeModel;

    public RootTreeNode(CategoryTreeNode... children) {
        for (CategoryTreeNode child : children) {
            insert(child, getChildCount());
        }
    }

    public boolean isLocalRoot() {
        return false;
    }

    public boolean isLocal() {
        return false;
    }

    public boolean isRemoteRoot() {
        return false;
    }

    public boolean isRemote() {
        return false;
    }

    public String getName() {
        return null;
    }

    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    public void setTreeModel(DefaultTreeModel treeModel) {
        this.treeModel = treeModel;
    }

    public Category getCategory() {
        throw new UnsupportedOperationException();
    }

    public RoutesListModel getRoutesListModel() {
        return null;
    }

    public void clearChildren() {
        throw new UnsupportedOperationException();
    }


    public CategoryTreeNode addChild(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void move(CategoryTreeNode parent) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void rename(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void delete() throws IOException {
        throw new UnsupportedOperationException();
    }

    public Route addRoute(String description, File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    public Route addRoute(String description, String fileUrl) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void moveRoute(Route route, CategoryTreeNode target) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void renameRoute(Route route, String description) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void deleteRoute(Route route) throws IOException {
        throw new UnsupportedOperationException();
    }
}
