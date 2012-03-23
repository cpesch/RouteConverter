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

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;

/**
 * A {@link MutableTreeNode} that encapsulates a {@link Category}.
 *
 * @author Christian Pesch
 */

public interface CategoryTreeNode extends MutableTreeNode {
    boolean isLocalRoot();
    boolean isLocal();
    boolean isRemoteRoot();
    boolean isRemote();
    Category getCategory();
    String getName();
    TreeNode[] getPath();

    RoutesListModel getRoutesListModel();
    void clearChildren();

    Route addRoute(String description, File file) throws IOException;
    Route addRoute(String description, String fileUrl) throws IOException;
    void moveRoute(Route route, CategoryTreeNode target) throws IOException;
    void deleteRoute(Route route) throws IOException;
}
