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
import java.util.List;

/**
 * A {@link CategoryTreeNode} that has a given set of {@link CategoryTreeNode} as its children.
 *
 * @author Christian Pesch
 */

public class RootTreeNode extends DefaultMutableTreeNode implements CategoryTreeNode {
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

    public Category getCategory() {
        throw new UnsupportedOperationException();
    }

    public List<Route> getRoutes() {
        return null;
    }
}
